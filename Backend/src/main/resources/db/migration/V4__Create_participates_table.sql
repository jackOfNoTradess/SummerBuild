CREATE TABLE participates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Ensure a user can only participate in an event once
    UNIQUE(user_id, event_id)
);

-- Create indexes for better query performance
CREATE INDEX idx_participates_user_id ON participates(user_id);
CREATE INDEX idx_participates_event_id ON participates(event_id);
CREATE INDEX idx_participates_user_event ON participates(user_id, event_id);
