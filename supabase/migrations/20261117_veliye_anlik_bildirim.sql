-- Dijital Kalkan – veliye anlık bildirim
-- Uyarı eklenince "uyari-bildirimi" fonksiyonunu çağırır; fonksiyon Firebase ile veliye bildirim gönderir.
-- Supabase panelinde: SQL Editor → New query → bu dosyanın tamamını yapıştır → Run
create extension if not exists pg_net with schema extensions;

alter table public.alerts add column if not exists notified_at timestamptz;

create or replace function public.notify_parent_push()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  perform net.http_post(
    url := 'https://nreqibswrqqhlsqweeue.supabase.co/functions/v1/uyari-bildirimi',
    body := jsonb_build_object('alert_id', new.id),
    headers := '{"Content-Type": "application/json"}'::jsonb,
    timeout_milliseconds := 5000
  );
  return new;
end $$;

drop trigger if exists alerts_notify_parent on public.alerts;
create trigger alerts_notify_parent after insert on public.alerts
  for each row execute function public.notify_parent_push();

revoke all on function public.notify_parent_push() from public, anon, authenticated;
