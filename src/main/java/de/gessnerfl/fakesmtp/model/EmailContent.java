package de.gessnerfl.fakesmtp.model;

import javax.persistence.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

@Entity
@Table(name = "email_content")
@SequenceGenerator(name = "email_part_generator", sequenceName = "email_content_sequence", allocationSize = 1)
public class EmailContent extends EmailPart {
    private static final Pattern CID_PATTERN = Pattern.compile("<img[^>]+src=(?:\"cid:([^\">]+)\"|'cid:([^'>]+)')");

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

    public String getRawData() {
        return super.getData();
    }

    public String getData() {
        var data = super.getData();
        if (data != null && data.contains("cid:")) {
            var matcher = CID_PATTERN.matcher(data);
            return matcher.replaceAll(this::replaceContentIdWithBase64DataWhenAvailable);
        }
        return data;
    }

    private String replaceContentIdWithBase64DataWhenAvailable(MatchResult mr) {
        return getEmail().getInlineImageByContentId(mr.group(1))
                .map(i -> mapInlineImage(mr, i))
                .orElseGet(mr::group);
    }

    private static String mapInlineImage(MatchResult mr, InlineImage i) {
        return mr.group().replace("cid:" + mr.group(1), "data:" + i.getContentType() + ";base64, " + i.getData());
    }
}
