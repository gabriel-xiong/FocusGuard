package com.focusguard.social

import com.focusguard.BuildConfig
import com.focusguard.state.DemoSocialProfileId
import com.focusguard.state.EarnedItUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.Locale

data class SocialProfile(
    val id: String,
    val username: String,
    val displayName: String,
    val petSpecies: String,
    val petStage: Int,
    val points: Int,
    val weeklyFocusMinutes: Int,
    val streakDays: Int,
)

data class SocialFriendRequest(
    val id: String,
    val requesterId: String,
    val addresseeId: String,
    val status: String,
    val requester: SocialProfile?,
    val addressee: SocialProfile?,
)

data class SocialSnapshot(
    val me: SocialProfile,
    val leaderboard: List<SocialProfile>,
    val friends: List<SocialProfile>,
    val incomingRequests: List<SocialFriendRequest>,
    val outgoingRequests: List<SocialFriendRequest>,
)

sealed class SocialActionResult {
    data object Success : SocialActionResult()
    data object NotConfigured : SocialActionResult()
    data object UserNotFound : SocialActionResult()
    data object CannotAddSelf : SocialActionResult()
    data object AlreadyConnected : SocialActionResult()
    data object UsernameTaken : SocialActionResult()
    data class Failure(val message: String) : SocialActionResult()
}

class SupabaseSocialRepository {
    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    suspend fun loadSnapshot(appState: EarnedItUiState): Result<SocialSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            requireConfigured()
            val profileId = appState.socialProfileId()
            val me = upsertProfile(appState)
            val requests = fetchRequests(profileId)
            val profileIds = requests
                .flatMap { listOf(it.requesterId, it.addresseeId) }
                .plus(profileId)
                .distinct()
            val profiles = fetchProfiles(profileIds).associateBy { it.id } + (me.id to me)
            val enriched = requests.map {
                it.copy(
                    requester = profiles[it.requesterId],
                    addressee = profiles[it.addresseeId],
                )
            }
            val friends = enriched
                .filter { it.status == "accepted" }
                .mapNotNull { request ->
                    val friendId = if (request.requesterId == profileId) request.addresseeId else request.requesterId
                    profiles[friendId]
                }
                .distinctBy { it.id }
                .sortedBy { it.username }
            val leaderboard = (friends + me)
                .distinctBy { it.id }
                .sortedWith(compareByDescending<SocialProfile> { it.points }.thenByDescending { it.weeklyFocusMinutes })

            SocialSnapshot(
                me = me,
                leaderboard = leaderboard,
                friends = friends,
                incomingRequests = enriched.filter { it.status == "pending" && it.addresseeId == profileId },
                outgoingRequests = enriched.filter { it.status == "pending" && it.requesterId == profileId },
            )
        }
    }

    suspend fun sendFriendRequest(appState: EarnedItUiState, rawUsername: String): SocialActionResult = withContext(Dispatchers.IO) {
        runCatching {
            requireConfigured()
            val profileId = appState.socialProfileId()
            upsertProfile(appState)
            val username = normalizeUsername(rawUsername)
            if (username.isBlank()) return@withContext SocialActionResult.UserNotFound

            val target = fetchProfileByUsername(username) ?: return@withContext SocialActionResult.UserNotFound
            if (target.id == profileId) return@withContext SocialActionResult.CannotAddSelf

            val existing = fetchRequests(profileId).firstOrNull {
                (it.requesterId == profileId && it.addresseeId == target.id) ||
                    (it.requesterId == target.id && it.addresseeId == profileId)
            }
            when (existing?.status) {
                "accepted" -> return@withContext SocialActionResult.AlreadyConnected
                "pending" -> {
                    if (existing.addresseeId == profileId) {
                        updateRequest(existing.id, "accepted")
                        return@withContext SocialActionResult.Success
                    }
                    return@withContext SocialActionResult.AlreadyConnected
                }
            }

            val body = JSONObject()
                .put("requester_id", profileId)
                .put("addressee_id", target.id)
                .put("status", "pending")
            request("POST", "/rest/v1/social_friend_requests", body, prefer = "return=minimal")
            SocialActionResult.Success
        }.getOrElse { SocialActionResult.Failure(it.message ?: "Friend request failed.") }
    }

    suspend fun acceptRequest(requestId: String): SocialActionResult = updateRequestStatus(requestId, "accepted")

    suspend fun declineRequest(requestId: String): SocialActionResult = updateRequestStatus(requestId, "declined")

    private suspend fun updateRequestStatus(requestId: String, status: String): SocialActionResult = withContext(Dispatchers.IO) {
        runCatching {
            requireConfigured()
            updateRequest(requestId, status)
            SocialActionResult.Success
        }.getOrElse { SocialActionResult.Failure(it.message ?: "Request update failed.") }
    }

    private fun requireConfigured() {
        if (!isConfigured) throw IllegalStateException("Supabase anon key is missing.")
    }

    private fun upsertProfile(appState: EarnedItUiState): SocialProfile {
        val profile = SocialProfile(
            id = appState.socialProfileId(),
            username = normalizeUsername(appState.profile.username),
            displayName = appState.profile.displayName,
            petSpecies = appState.pet.species,
            petStage = appState.pet.stage.coerceIn(1, 5),
            points = appState.points,
            weeklyFocusMinutes = appState.weeklyFocusMinutes,
            streakDays = appState.streakDays,
        )
        val body = JSONObject()
            .put("id", profile.id)
            .put("username", profile.username)
            .put("display_name", profile.displayName)
            .put("pet_species", profile.petSpecies)
            .put("pet_stage", profile.petStage)
            .put("points", profile.points)
            .put("weekly_focus_minutes", profile.weeklyFocusMinutes)
            .put("streak_days", profile.streakDays)
        request(
            method = "POST",
            path = "/rest/v1/social_profiles?on_conflict=id",
            body = body,
            prefer = "resolution=merge-duplicates,return=minimal",
        )
        return profile
    }

    private fun fetchProfileByUsername(username: String): SocialProfile? {
        val array = request(
            method = "GET",
            path = "/rest/v1/social_profiles?username=eq.${username.encodeUrl()}&select=*",
        ).asArray()
        return array.objects().firstOrNull()?.toProfile()
    }

    private fun fetchProfiles(ids: List<String>): List<SocialProfile> {
        if (ids.isEmpty()) return emptyList()
        val inList = ids.distinct().joinToString(",") { it.encodeUrl() }
        return request(
            method = "GET",
            path = "/rest/v1/social_profiles?id=in.($inList)&select=*",
        ).asArray().objects().map { it.toProfile() }
    }

    private fun fetchRequests(userId: String): List<SocialFriendRequest> =
        request(
            method = "GET",
            path = "/rest/v1/social_friend_requests?or=(requester_id.eq.${userId.encodeUrl()},addressee_id.eq.${userId.encodeUrl()})&status=in.(pending,accepted)&select=*&order=created_at.desc",
        ).asArray().objects().map { it.toFriendRequest() }

    private fun updateRequest(requestId: String, status: String) {
        request(
            method = "PATCH",
            path = "/rest/v1/social_friend_requests?id=eq.${requestId.encodeUrl()}",
            body = JSONObject().put("status", status),
            prefer = "return=minimal",
        )
    }

    private fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        prefer: String? = null,
    ): String {
        val connection = URL("${BuildConfig.SUPABASE_URL.trimEnd('/')}$path").openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
        connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        if (prefer != null) connection.setRequestProperty("Prefer", prefer)
        if (body != null) {
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
        connection.disconnect()

        if (responseCode !in 200..299) {
            val message = runCatching { JSONObject(response).optString("message") }.getOrNull()
                ?: response.ifBlank { "HTTP $responseCode" }
            throw IllegalStateException(message)
        }
        return response
    }
}

fun normalizeUsername(value: String): String =
    value.trim().lowercase(Locale.US).replace(Regex("[^a-z0-9_\\.]"), "").take(24)

fun EarnedItUiState.socialProfileId(): String =
    if (settings.demoModeEnabled) DemoSocialProfileId else profile.id

private fun String.encodeUrl(keepComma: Boolean = false): String {
    val encoded = URLEncoder.encode(this, "UTF-8").replace("+", "%20")
    return if (keepComma) encoded.replace("%2C", ",") else encoded
}

private fun String.asArray(): JSONArray =
    if (isBlank()) JSONArray() else JSONArray(this)

private fun JSONArray.objects(): List<JSONObject> =
    (0 until length()).mapNotNull { optJSONObject(it) }

private fun JSONObject.toProfile(): SocialProfile =
    SocialProfile(
        id = optString("id"),
        username = optString("username"),
        displayName = optString("display_name", optString("username")),
        petSpecies = optString("pet_species", "kitsu"),
        petStage = optInt("pet_stage", 1).coerceIn(1, 5),
        points = optInt("points", 0),
        weeklyFocusMinutes = optInt("weekly_focus_minutes", 0),
        streakDays = optInt("streak_days", 0),
    )

private fun JSONObject.toFriendRequest(): SocialFriendRequest =
    SocialFriendRequest(
        id = optString("id"),
        requesterId = optString("requester_id"),
        addresseeId = optString("addressee_id"),
        status = optString("status"),
        requester = null,
        addressee = null,
    )
