ALTER TABLE ts_user_behavior_event
    ADD COLUMN IF NOT EXISTS content_version Nullable(UInt32)
    AFTER resource_id;

ALTER TABLE ts_user_behavior_event
    ADD COLUMN IF NOT EXISTS tag_ids Array(UInt64) DEFAULT []
    AFTER content_version;

ALTER TABLE ts_user_behavior_event
    ADD COLUMN IF NOT EXISTS tag_scores Array(Float32) DEFAULT []
    AFTER tag_ids;
