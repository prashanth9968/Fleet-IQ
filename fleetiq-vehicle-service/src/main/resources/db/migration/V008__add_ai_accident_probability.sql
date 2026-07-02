-- V008__add_ai_accident_probability.sql
-- Add missing AI accident probability column to driver safety scores

ALTER TABLE driver_safety_scores 
ADD COLUMN IF NOT EXISTS ai_accident_probability DECIMAL(5, 4);
