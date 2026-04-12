package de.gessnerfl.fakesmtp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "email_attachment")
public class EmailAttachment {
    @Id
    @SequenceGenerator(name = "email_attachment_generator", sequenceName = "email_attachment_sequence", allocationSize = 1)
    @GeneratedValue(generator = "email_attachment_generator")
    private Long id;

    @ManyToOne(fetch= FetchType.LAZY, optional = false)
    @JoinColumn(name="email")
    private Email email;

    @Column(name="filename", nullable = false, length = 1024)
    @Basic(optional = false)
    private String filename;

    @Lob
    @Column(name="data", nullable = false)
    @Basic(optional = false)
    private byte[] data;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 64)
    @Basic(optional = false)
    private EmailPartProcessingStatus processingStatus = EmailPartProcessingStatus.AVAILABLE;

    @Column(name = "processing_message", length = 1024)
    private String processingMessage;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonIgnore
    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
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
