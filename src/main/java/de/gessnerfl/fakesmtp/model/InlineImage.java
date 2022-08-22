package de.gessnerfl.fakesmtp.model;

import javax.persistence.*;

@Entity
@Table(name = "email_inline_image")
public class InlineImage {

    @Id
    @SequenceGenerator(name = "email_inline_image_generator", sequenceName = "email_inline_image_sequence", allocationSize = 1)
    @GeneratedValue(generator = "email_inline_image_generator")
    private Long id;

    @ManyToOne(fetch= FetchType.LAZY, optional = false)
    @JoinColumn(name="email")
    private Email email;

    @Column(name="content_id", length = 255, nullable = false)
    @Basic(optional = false)
    private String contentId;

    @Column(name="content_type", length = 255, nullable = false)
    @Basic(optional = false)
    private String contentType;

    @Lob
    @Column(name="data", nullable = false)
    @Basic(optional = false)
    private String data;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

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

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
