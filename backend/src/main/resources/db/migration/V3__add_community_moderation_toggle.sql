-- V3__add_community_moderation_toggle.sql
-- Add moderation toggle column to communities table

ALTER TABLE communities ADD COLUMN IF NOT EXISTS requires_moderation BOOLEAN NOT NULL DEFAULT FALSE;
