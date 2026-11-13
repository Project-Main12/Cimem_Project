-- =====================================================================
-- Dijital Kalkan – veli/çocuk bağlantısı ve risk uyarıları
-- Supabase'e 13.11.2026'da "veli_cocuk_baglantisi_ve_uyarilar" adıyla uygulandı.
-- İlke: Mesaj metni, gönderen kişi, lakap listesi BU VERİTABANINA GİRMEZ.
-- =====================================================================

create type public.user_role as enum ('parent', 'child');

create table public.profiles (
  id uuid primary key references auth.users on delete cascade,
  role public.user_role not null,
  display_name text check (char_length(display_name) <= 40),
  created_at timestamptz not null default now()
);

create table public.pairing_codes (
  code text primary key check (code ~ '^[0-9]{6}$'),
  parent_id uuid not null references auth.users on delete cascade,
  expires_at timestamptz not null,
  used_at timestamptz,
  created_at timestamptz not null default now()
);
create index pairing_codes_parent_idx on public.pairing_codes (parent_id);

create table public.pairing_attempts (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users on delete cascade,
  attempted_at timestamptz not null default now()
);
create index pairing_attempts_user_idx on public.pairing_attempts (user_id, attempted_at);

create table public.family_links (
  id uuid primary key default gen_random_uuid(),
  parent_id uuid not null references auth.users on delete cascade,
  child_id uuid not null unique references auth.users on delete cascade,
  child_label text not null check (char_length(child_label) between 1 and 40),
  created_at timestamptz not null default now()
);
create index family_links_parent_idx on public.family_links (parent_id);

create table public.alerts (
  id bigint generated always as identity primary key,
  child_id uuid not null references auth.users on delete cascade,
  parent_id uuid not null references auth.users on delete cascade,
  category text not null check (category in
    ('HAKARET','KUFUR','SIBER_ZORBALIK','TEHDIT','UYGUNSUZ_YAKLASIM','KENDINE_ZARAR')),
  level text not null check (level in ('DUSUK','ORTA','YUKSEK')),
  source_app text not null check (char_length(source_app) <= 120),
  method text not null default 'ANAHTAR_KELIME' check (char_length(method) <= 40),
  detected_at timestamptz not null default now(),
  read_at timestamptz,
  created_at timestamptz not null default now()
);
create index alerts_parent_idx on public.alerts (parent_id, created_at desc);
create index alerts_child_idx on public.alerts (child_id, created_at desc);

create table public.push_tokens (
  user_id uuid primary key references auth.users on delete cascade,
  fcm_token text not null check (char_length(fcm_token) <= 4096),
  updated_at timestamptz not null default now()
);

alter table public.profiles         enable row level security;
alter table public.pairing_codes    enable row level security;
alter table public.pairing_attempts enable row level security;
alter table public.family_links     enable row level security;
alter table public.alerts           enable row level security;
alter table public.push_tokens      enable row level security;

create policy "profil_oku" on public.profiles for select to authenticated
  using (id = (select auth.uid()));
create policy "profil_olustur" on public.profiles for insert to authenticated
  with check (
    id = (select auth.uid())
    and (role = 'child' or coalesce(((select auth.jwt()) ->> 'is_anonymous')::boolean, false) = false)
  );
create policy "profil_guncelle" on public.profiles for update to authenticated
  using (id = (select auth.uid())) with check (id = (select auth.uid()));
revoke update on public.profiles from authenticated;
grant update (display_name) on public.profiles to authenticated;

create policy "baglanti_oku" on public.family_links for select to authenticated
  using (parent_id = (select auth.uid()) or child_id = (select auth.uid()));
create policy "baglanti_kaldir" on public.family_links for delete to authenticated
  using (parent_id = (select auth.uid()) or child_id = (select auth.uid()));

create or replace function public.set_alert_parent()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  select fl.parent_id into new.parent_id
    from public.family_links fl where fl.child_id = new.child_id;
  if new.parent_id is null then
    raise exception 'Bu cihaz bir veliye bağlı değil';
  end if;
  return new;
end $$;
create trigger alerts_set_parent before insert on public.alerts
  for each row execute function public.set_alert_parent();

create policy "uyari_ekle" on public.alerts for insert to authenticated
  with check (
    child_id = (select auth.uid())
    and exists (select 1 from public.family_links fl where fl.child_id = (select auth.uid()))
  );
create policy "uyari_oku" on public.alerts for select to authenticated
  using (parent_id = (select auth.uid()) or child_id = (select auth.uid()));
create policy "uyari_okundu" on public.alerts for update to authenticated
  using (parent_id = (select auth.uid())) with check (parent_id = (select auth.uid()));
revoke update on public.alerts from authenticated;
grant update (read_at) on public.alerts to authenticated;

create policy "token_yonet" on public.push_tokens for all to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

create or replace function public.create_pairing_code()
returns text language plpgsql security definer set search_path = '' as $$
declare
  v_uid uuid := auth.uid();
  v_code text;
begin
  if v_uid is null then raise exception 'Giriş yapılmamış'; end if;
  if coalesce((auth.jwt() ->> 'is_anonymous')::boolean, false) then
    raise exception 'Kod üretmek için veli hesabıyla giriş yapılmalı';
  end if;
  if not exists (select 1 from public.profiles where id = v_uid and role = 'parent') then
    raise exception 'Bu hesap veli hesabı değil';
  end if;
  delete from public.pairing_codes where parent_id = v_uid;
  delete from public.pairing_codes where expires_at < now();
  loop
    v_code := lpad((floor(random() * 1000000))::int::text, 6, '0');
    exit when not exists (select 1 from public.pairing_codes where code = v_code);
  end loop;
  insert into public.pairing_codes (code, parent_id, expires_at)
    values (v_code, v_uid, now() + interval '10 minutes');
  return v_code;
end $$;

create or replace function public.redeem_pairing_code(p_code text, p_child_label text)
returns uuid language plpgsql security definer set search_path = '' as $$
declare
  v_uid uuid := auth.uid();
  v_parent uuid;
  v_link uuid;
begin
  if v_uid is null then raise exception 'Giriş yapılmamış'; end if;
  if exists (select 1 from public.profiles where id = v_uid and role = 'parent') then
    raise exception 'Veli hesabı çocuk cihazı olarak bağlanamaz';
  end if;
  if (select count(*) from public.pairing_attempts
        where user_id = v_uid and attempted_at > now() - interval '1 hour') >= 10 then
    raise exception 'Çok fazla deneme. Bir saat sonra tekrar deneyin';
  end if;
  insert into public.pairing_attempts (user_id) values (v_uid);
  select parent_id into v_parent from public.pairing_codes
    where code = p_code and used_at is null and expires_at > now()
    for update;
  if v_parent is null then raise exception 'Kod geçersiz veya süresi dolmuş'; end if;
  insert into public.profiles (id, role) values (v_uid, 'child')
    on conflict (id) do nothing;
  delete from public.family_links where child_id = v_uid;
  insert into public.family_links (parent_id, child_id, child_label)
    values (v_parent, v_uid, left(coalesce(nullif(trim(p_child_label), ''), 'Çocuk'), 40))
    returning id into v_link;
  update public.pairing_codes set used_at = now() where code = p_code;
  return v_link;
end $$;

revoke all on function public.create_pairing_code() from public, anon;
revoke all on function public.redeem_pairing_code(text, text) from public, anon;
grant execute on function public.create_pairing_code() to authenticated;
grant execute on function public.redeem_pairing_code(text, text) to authenticated;
revoke all on function public.set_alert_parent() from public, anon, authenticated;

alter publication supabase_realtime add table public.alerts;
