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
}
