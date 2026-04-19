create extension if not exists pgcrypto;

create table if not exists public.social_profiles (
    id uuid primary key,
    username text not null unique,
    display_name text not null default '',
    pet_species text not null default 'kitsu',
    pet_stage integer not null default 1 check (pet_stage between 1 and 5),
    points integer not null default 0,
    weekly_focus_minutes integer not null default 0,
    streak_days integer not null default 0,
    updated_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    constraint social_profiles_username_format check (username ~ '^[a-z0-9_.]{3,24}$')
);

alter table public.social_profiles
    drop constraint if exists social_profiles_pet_stage_check;

alter table public.social_profiles
    add constraint social_profiles_pet_stage_check
    check (pet_stage between 1 and 5);

create table if not exists public.social_friend_requests (
    id uuid primary key default gen_random_uuid(),
    requester_id uuid not null references public.social_profiles(id) on delete cascade,
    addressee_id uuid not null references public.social_profiles(id) on delete cascade,
    status text not null default 'pending' check (status in ('pending', 'accepted', 'declined')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint social_friend_requests_no_self check (requester_id <> addressee_id)
);

create unique index if not exists social_friend_requests_pair_idx
on public.social_friend_requests (
    least(requester_id, addressee_id),
    greatest(requester_id, addressee_id)
)
where status in ('pending', 'accepted');

create index if not exists social_friend_requests_requester_idx
on public.social_friend_requests (requester_id, status);

create index if not exists social_friend_requests_addressee_idx
on public.social_friend_requests (addressee_id, status);

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists social_profiles_set_updated_at on public.social_profiles;
create trigger social_profiles_set_updated_at
before update on public.social_profiles
for each row execute function public.set_updated_at();

drop trigger if exists social_friend_requests_set_updated_at on public.social_friend_requests;
create trigger social_friend_requests_set_updated_at
before update on public.social_friend_requests
for each row execute function public.set_updated_at();

alter table public.social_profiles disable row level security;
alter table public.social_friend_requests disable row level security;

grant usage on schema public to anon, authenticated;
grant select, insert, update on public.social_profiles to anon, authenticated;
grant select, insert, update on public.social_friend_requests to anon, authenticated;
