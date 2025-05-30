CREATE TABLE users (
    id UUID PRIMARY KEY,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ADMIN', 'ORGANIZER')),
    gender VARCHAR(10) NOT NULL CHECK (gender IN ('MALE', 'FEMALE', 'OTHERS')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -- Create unique index on user_uuid
-- CREATE UNIQUE INDEX idx_user_uuid ON users(user_uuid);

-- -- Index for role and gender
-- CREATE INDEX idx_users_role ON users(role);
-- CREATE INDEX idx_users_gender ON users(gender);
