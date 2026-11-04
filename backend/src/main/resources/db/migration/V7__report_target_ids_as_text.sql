ALTER TABLE reports
    ALTER COLUMN target_id TYPE VARCHAR(100)
    USING target_id::text;
