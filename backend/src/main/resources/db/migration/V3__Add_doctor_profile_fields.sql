-- Add missing fields to doctor_profiles table
ALTER TABLE doctor_profiles 
ADD COLUMN IF NOT EXISTS specialty VARCHAR(255),
ADD COLUMN IF NOT EXISTS bio TEXT,
ADD COLUMN IF NOT EXISTS license_number VARCHAR(255),
ADD COLUMN IF NOT EXISTS clinic_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS experience_years INTEGER,
ADD COLUMN IF NOT EXISTS profile_picture VARCHAR(500),
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add missing fields to patient_profiles table (if not already present)
ALTER TABLE patient_profiles 
ADD COLUMN IF NOT EXISTS profile_picture VARCHAR(500);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_doctor_profiles_specialty ON doctor_profiles(specialty);
CREATE INDEX IF NOT EXISTS idx_doctor_profiles_clinic_name ON doctor_profiles(clinic_name);
CREATE INDEX IF NOT EXISTS idx_patient_profiles_date_of_birth ON patient_profiles(date_of_birth);
