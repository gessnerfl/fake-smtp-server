package de.gessnerfl.fakesmtp.model;

import javax.persistence.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

@Entity
@Table(name = "email_content")
public class EmailContent {
    private static final Pattern CID_PATTERN = Pattern.compile("(< *img.*src=[\"']cid:)(.*)([\"'].*\\/?>)");
    @Id
    @SequenceGenerator(name = "email_content_generator", sequenceName = "email_content_sequence", allocationSize = 1)
    @GeneratedValue(generator = "email_content_generator")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "email")
    private Email email;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    @Basic(optional = false)
    private ContentType contentType;

    @Lob
    @Column(name = "data", nullable = false)
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

    public String getRawData(){
        return data;
    }

    public String getData() {
        if (data != null && data.contains("cid:")) {
            var matcher = CID_PATTERN.matcher(data);
            return matcher.replaceAll(this::replaceContentIdWithBase64DataWhenAvailable);
        }
        return data;
    }

    private String replaceContentIdWithBase64DataWhenAvailable(MatchResult mr) {
        return email.getInlineImageByContentId(mr.group(2))
                .map(i -> mapInlineImage(mr, i))
                .orElseGet(mr::group);
    }

    private static String mapInlineImage(MatchResult mr, InlineImage i) {
        return mr.group().replace("cid:" + mr.group(2), "data:" + i.getContentType() + ";base64, " + i.getData());
    }

    public void setData(String data) {
        this.data = data;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }
}
