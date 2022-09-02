package de.gessnerfl.fakesmtp.model;

import javax.persistence.*;

@Entity
@Table(name = "email_content")
@SequenceGenerator(name = "email_part_generator", sequenceName = "email_content_sequence", allocationSize = 1)
public class EmailContent extends EmailPart {

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    @Basic(optional = false)
    private ContentType contentType;

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }
}
