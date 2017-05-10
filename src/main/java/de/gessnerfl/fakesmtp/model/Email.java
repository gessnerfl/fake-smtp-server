package de.gessnerfl.fakesmtp.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "email")
public class Email {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
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

    @Column(name="received_at", nullable = false)
    @Basic(optional = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date receivedAt;

    @Lob
    @Column(name="content", nullable = false)
    @Basic(optional = false)
    private String content;

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

    public Date getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Date receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
}
