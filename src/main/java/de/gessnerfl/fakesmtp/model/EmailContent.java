package de.gessnerfl.fakesmtp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "email_content")
@SequenceGenerator(name = "email_part_generator", sequenceName = "email_content_sequence", allocationSize = 1)
public class EmailContent extends EmailPart {

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    @Basic(optional = false)
    private ContentType contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 64)
    @Basic(optional = false)
    private EmailPartProcessingStatus processingStatus = EmailPartProcessingStatus.AVAILABLE;

    @Column(name = "processing_message", length = 1024)
    private String processingMessage;

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public EmailPartProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(EmailPartProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public String getProcessingMessage() {
        return processingMessage;
    }

    public void setProcessingMessage(String processingMessage) {
        this.processingMessage = processingMessage;
    }
}
