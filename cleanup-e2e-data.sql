-- Cleanup script to remove all E2E test data from the database
-- This script deletes all test users and their related data
-- To run this script, use the following command:
-- cd /Users/ivettt/Desktop/Individual_S3 && docker exec -i superdoc-db-local psql -U superdoc -d superdocdb < cleanup-e2e-data.sql

BEGIN;

-- Delete appointments created by test users
DELETE FROM appointments
WHERE doctor_id IN (
    SELECT dp.id 
    FROM doctor_profiles dp
    JOIN users u ON dp.user_id = u.id
    WHERE u.email LIKE '%booking.%@test.com'
)
OR patient_id IN (
    SELECT pp.id 
    FROM patient_profiles pp
    JOIN users u ON pp.user_id = u.id
    WHERE u.email LIKE '%booking.%@test.com'
);

-- Delete prescriptions for test users
DELETE FROM prescriptions
WHERE patient_id IN (
    SELECT id FROM users WHERE email LIKE '%booking.%@test.com'
)
OR doctor_id IN (
    SELECT id FROM users WHERE email LIKE '%booking.%@test.com'
);

-- Delete doctor profiles for test users (CASCADE will handle related data)
DELETE FROM doctor_profiles
WHERE user_id IN (
    SELECT id FROM users WHERE email LIKE '%booking.%@test.com'
);

-- Delete patient profiles for test users (CASCADE will handle related data)
DELETE FROM patient_profiles
WHERE user_id IN (
    SELECT id FROM users WHERE email LIKE '%booking.%@test.com'
);

-- Finally, delete test users
DELETE FROM users
WHERE email LIKE '%booking.%@test.com';

COMMIT;

-- Show summary of remaining test data (if any)
SELECT 
    'Remaining test users' as category,
    COUNT(*) as count
FROM users 
WHERE email LIKE '%booking.%@test.com'
UNION ALL
SELECT 
    'Remaining test appointments' as category,
    COUNT(*) as count
FROM appointments a
JOIN doctor_profiles dp ON a.doctor_id = dp.id
JOIN users u ON dp.user_id = u.id
WHERE u.email LIKE '%booking.%@test.com'
UNION ALL
SELECT 
    'Remaining test prescriptions' as category,
    COUNT(*) as count
FROM prescriptions p
JOIN users u ON p.patient_id = u.id
WHERE u.email LIKE '%booking.%@test.com';
