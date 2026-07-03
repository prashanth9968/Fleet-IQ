-- V009__add_remaining_ai_columns.sql
-- Add remaining missing AI columns to driver safety scores

ALTER TABLE driver_safety_scores 
ADD COLUMN IF NOT EXISTS ai_predicted_fatigue_probability DECIMAL(5, 4),
ADD COLUMN IF NOT EXISTS ai_insurance_risk_score DECIMAL(5, 2),
ADD COLUMN IF NOT EXISTS coaching_suggestions JSONB,
ADD COLUMN IF NOT EXISTS trend_data JSONB;
