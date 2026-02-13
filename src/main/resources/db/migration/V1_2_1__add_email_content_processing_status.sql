ALTER TABLE email_content ADD COLUMN processing_status VARCHAR(64) NOT NULL DEFAULT 'AVAILABLE';
ALTER TABLE email_content ADD COLUMN processing_message VARCHAR(1024);
