ALTER TABLE email_attachment ADD COLUMN processing_status VARCHAR(64) NOT NULL DEFAULT 'AVAILABLE';
ALTER TABLE email_attachment ADD COLUMN processing_message VARCHAR(1024);

ALTER TABLE email_inline_image ADD COLUMN processing_status VARCHAR(64) NOT NULL DEFAULT 'AVAILABLE';
ALTER TABLE email_inline_image ADD COLUMN processing_message VARCHAR(1024);
