package de.gessnerfl.fakesmtp.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "email")
public class Email {
    @Id
    @SequenceGenerator(name = "email_generator", sequenceName = "email_sequence", allocationSize = 1)
    @GeneratedValue(generator = "email_generator")
    private Long id;

    @Column(name="from_address", length = 255, nullable = false)
    @Basic(optional = false)
    private String fromAddress;

    @Column(name="to_address", length = 255, nullable = false)
    @Basic(optional = false)
    private String toAddress;

    @Lob
    @Column(name="subject", nullable = false)
    @Basic(optional = false)
    private String subject;

    @Column(name="received_on", nullable = false)
    @Basic(optional = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date receivedOn;

    @Lob
    @Column(name="raw_data", nullable = false)
    @Basic(optional = false)
    private String rawData;

    @Lob
    @Column(name="content", nullable = false)
    @Basic(optional = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name="content_type", nullable = false)
    @Basic(optional = false)
    private ContentType contentType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Date getReceivedOn() {
        return receivedOn;
    }

    public void setReceivedOn(Date receivedOn) {
        this.receivedOn = receivedOn;
    }

    public void setRawData(String rawData){
        this.rawData = rawData;
    }

    public String getRawData() {
        return rawData;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Email email = (Email) o;

        return id.equals(email.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static class Builder {
        private String fromAddress;
        private String toAddress;
        private Date receivedOn;
        private String subject;
        private String rawData;
        private String content;
        private ContentType contentType;

        public Builder fromAddress(String fromAddress) {
            this.fromAddress = fromAddress;
            return this;
        }
        public Builder toAddress(String toAddress) {
            this.toAddress = toAddress;
            return this;
        }
        public Builder receivedOn(Date receivedOn) {
            this.receivedOn = receivedOn;
            return this;
        }
        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }
        public Builder rawData(String rawData) {
            this.rawData = rawData;
            return this;
        }
        public Builder content(String content) {
            this.content = content;
            return this;
        }
        public Builder contentType(ContentType contentType) {
            this.contentType = contentType;
            return this;
        }

        public Email build() {
            Email email = new Email();
            email.setFromAddress(this.fromAddress);
            email.setToAddress(this.toAddress);
            email.setReceivedOn(this.receivedOn);
            email.setSubject(this.subject);
            email.setRawData(this.rawData);
            if (this.content != null && !this.content.isEmpty())
                email.setContent(this.content);
            else
                email.setContent(this.rawData);
            email.setContentType(this.contentType);
            return email;
        }
    }
}
