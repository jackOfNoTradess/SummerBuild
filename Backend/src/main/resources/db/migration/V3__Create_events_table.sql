CREATE TABLE events (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    host_id UUID NOT NULL REFERENCES users(id),
    capacity INTEGER,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    description VARCHAR(255),
    tag TEXT[],
    pic_path VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on host_id for better query performance
CREATE INDEX idx_events_host_id ON events(host_id);

-- Create index on start_time for date-based queries
CREATE INDEX idx_events_start_time ON events(start_time);

-- Create index on end_time for date-based queries
CREATE INDEX idx_events_end_time ON events(end_time);
