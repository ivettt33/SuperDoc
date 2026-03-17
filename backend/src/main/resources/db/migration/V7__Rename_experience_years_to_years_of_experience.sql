-- V7: Rename experience_years to years_of_experience to match Hibernate entity
-- This fixes the column name mismatch between the database and DoctorProfileEntity

DO $$
BEGIN
    -- Rename experience_years to years_of_experience (Hibernate uses 'years_of_experience')
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'doctor_profiles' AND column_name = 'experience_years')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'doctor_profiles' AND column_name = 'years_of_experience') THEN
        ALTER TABLE doctor_profiles RENAME COLUMN experience_years TO years_of_experience;
    END IF;
END $$;
