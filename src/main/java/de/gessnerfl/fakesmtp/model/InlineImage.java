package de.gessnerfl.fakesmtp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "email_inline_image")
@SequenceGenerator(name = "email_part_generator", sequenceName = "email_inline_image_sequence", allocationSize = 1)
public class InlineImage extends EmailPart {
    @Column(name = "content_id", length = 255, nullable = false)
    @Basic(optional = false)
    private String contentId;

    @Column(name = "content_type", nullable = false)
    @Basic(optional = false)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 64)
    @Basic(optional = false)
    private EmailPartProcessingStatus processingStatus = EmailPartProcessingStatus.AVAILABLE;

    @Column(name = "processing_message", length = 1024)
    private String processingMessage;

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
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
