-- Step 1: Create the bucket
insert into storage.buckets (id, name, public) 
values ('event-pictures', 'event-pictures', true)
on conflict do nothing;

-- Step 2: Clear existing policies (optional)
DROP POLICY IF EXISTS "Allow read for all" ON storage.objects;
DROP POLICY IF EXISTS "Restrict write to owner" ON storage.objects;

-- Step 3: Allow anyone to read
CREATE POLICY "Allow read for all"
  ON storage.objects
  FOR SELECT
  USING (bucket_id = 'event-pictures');

-- Step 4: Allow insert/update/delete only for owner
CREATE POLICY "Restrict write to owner"
  ON storage.objects
  FOR ALL
  TO authenticated
  USING (
    bucket_id = 'event-pictures' AND
    metadata->>'owner' = auth.uid()::text
  )
  WITH CHECK (
    bucket_id = 'event-pictures' AND
    metadata->>'owner' = auth.uid()::text
  );
