CREATE TABLE email (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_address VARCHAR(255) NOT NULL,
    to_address VARCHAR(255) NOT NULL,
    subject TEXT NOT NULL,
    received_at TIMESTAMP NOT NULL,
    content TEXT NOT NULL
);