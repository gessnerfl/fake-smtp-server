package de.gessnerfl.fakesmtp.service;

import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailContent;
import de.gessnerfl.fakesmtp.model.InlineImage;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

@Service
public class HtmlContentRenderer {
    private static final Pattern CID_PATTERN = Pattern.compile("<img[^>]+src=(?:\"cid:([^\">]+)\"|'cid:([^'>]+)')");

    public String render(EmailContent content) {
        if (content.getContentType() == ContentType.HTML) {
            return harmonizeHtmlContent(content);
        }
        return content.getData();
    }

    private String harmonizeHtmlContent(EmailContent content) {
        return Optional.ofNullable(content.getData())
                .map(c -> replaceCidImageSourcesWithInlineImages(c, content.getEmail()))
                .map(this::parseHtmlHarmonizeAndAppendBootstrapCss)
                .orElse("");
    }

    private String replaceCidImageSourcesWithInlineImages(String html, Email email) {
        if (html.contains("cid:")) {
            var matcher = CID_PATTERN.matcher(html);
            return matcher.replaceAll(mr -> replaceContentIdWithBase64DataWhenAvailable(mr, email));
        }
        return html;
    }

    private String parseHtmlHarmonizeAndAppendBootstrapCss(String html) {
        var doc = Jsoup.parse(html);
        doc = doc.normalise();

        var bootstrapCss = doc.createElement("link")
                .attr("rel", "stylesheet")
                .attr("href", "/webjars/bootstrap/5.2.0/css/bootstrap.min.css");
        doc.head().insertChildren(0, bootstrapCss);
        return doc.html();
    }

    private String replaceContentIdWithBase64DataWhenAvailable(MatchResult mr, Email email) {
        return email.getInlineImageByContentId(mr.group(1))
                .map(i -> mapInlineImage(mr, i))
                .orElseGet(mr::group);
    }

    private static String mapInlineImage(MatchResult mr, InlineImage i) {
        return mr.group().replace("cid:" + mr.group(1), "data:" + i.getContentType() + ";base64, " + i.getData());
    }
}
