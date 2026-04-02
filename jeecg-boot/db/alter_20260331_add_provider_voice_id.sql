-- Add provider voice id mapping for TTS vendors (e.g. MiniMax voice_id)
ALTER TABLE ts_voice_profile
    ADD COLUMN provider_voice_id VARCHAR(100) NULL COMMENT 'TTS provider voice id' AFTER name;

