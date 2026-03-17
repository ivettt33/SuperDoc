-- V6: Align schema with Hibernate entity naming and add missing columns
-- This migration renames columns to match Hibernate naming and adds missing fields

-- Rename doctor_profiles columns to match Hibernate entity naming
DO $$
BEGIN
    -- Rename specialty to specialization (Hibernate uses 'specialization')
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'doctor_profiles' AND column_name = 'specialty')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'doctor_profiles' AND column_name = 'specialization') THEN
        ALTER TABLE doctor_profiles RENAME COLUMN specialty TO specialization;
    END IF;
    
    -- Rename profile_picture to profile_photo_url (Hibernate uses 'profile_photo_url')
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'doctor_profiles' AND column_name = 'profile_picture')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'doctor_profiles' AND column_name = 'profile_photo_url') THEN
        ALTER TABLE doctor_profiles RENAME COLUMN profile_picture TO profile_photo_url;
    END IF;
END $$;

-- Add missing columns to doctor_profiles (from DoctorProfile entity)
ALTER TABLE doctor_profiles 
ADD COLUMN IF NOT EXISTS location VARCHAR(255),
ADD COLUMN IF NOT EXISTS opening_hours TIME,
ADD COLUMN IF NOT EXISTS closing_hours TIME,
ADD COLUMN IF NOT EXISTS is_absent BOOLEAN NOT NULL DEFAULT false;

-- Add missing columns to users table (from UserEntity - these already exist but document them)
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS password_reset_token VARCHAR(255),
ADD COLUMN IF NOT EXISTS password_reset_expires_at TIMESTAMP;

-- Update indexes to match new column names
DROP INDEX IF EXISTS idx_doctor_profiles_specialty;
CREATE INDEX IF NOT EXISTS idx_doctor_profiles_specialization ON doctor_profiles(specialization);
