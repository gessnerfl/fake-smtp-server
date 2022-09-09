package de.gessnerfl.fakesmtp.service;

import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailContent;
import de.gessnerfl.fakesmtp.model.InlineImage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class HtmlContentRenderer {
    private static final Pattern CID_PATTERN = Pattern.compile("<img[^>]+src=(?:\"cid:([^\">]+)\"|'cid:([^'>]+)')");
    static final String BOOTSTRAP_VERSION = "bootstrap.version";

    private final BuildProperties buildProperties;

    @Autowired
    public HtmlContentRenderer(ApplicationContext applicationContext) {
        this.buildProperties = applicationContext.getBean(BuildProperties.class);
    }

    public String render(EmailContent content) {
        if (content.getContentType() == ContentType.HTML) {
            return harmonizeHtmlContent(content);
        }
        if (content.getContentType() == ContentType.PLAIN) {
            return convertLineBreaksToParagraphs(content);
        }
        return content.getData();
    }

    private String harmonizeHtmlContent(EmailContent content) {
        return Optional.ofNullable(content.getData())
                .map(c -> replaceCidImageSourcesWithInlineImages(c, content.getEmail()))
                .map(this::harmonizeHtmlDocument)
                .orElse("");
    }

    private String replaceCidImageSourcesWithInlineImages(String html, Email email) {
        if (html.contains("cid:")) {
            var matcher = CID_PATTERN.matcher(html);
            return matcher.replaceAll(mr -> replaceContentIdWithBase64DataWhenAvailable(mr, email));
        }
        return html;
    }

    private String harmonizeHtmlDocument(String html) {
        var doc = Jsoup.parse(html);
        doc = doc.normalise();
        appendBootstrapCss(doc);
        return doc.html();
    }

    private void appendBootstrapCss(Document doc) {
        var bootstrapVersion = buildProperties.get(BOOTSTRAP_VERSION);
        if (bootstrapVersion != null) {
            var bootstrapCss = doc.createElement("link")
                    .attr("rel", "stylesheet")
                    .attr("href", "/webjars/bootstrap/" + bootstrapVersion + "/css/bootstrap.min.css");
            doc.head().insertChildren(0, bootstrapCss);
        }
    }

    private String replaceContentIdWithBase64DataWhenAvailable(MatchResult mr, Email email) {
        return email.getInlineImageByContentId(mr.group(1))
                .map(i -> mapInlineImage(mr, i))
                .orElseGet(mr::group);
    }

    private static String mapInlineImage(MatchResult mr, InlineImage i) {
        return mr.group().replace("cid:" + mr.group(1), "data:" + i.getContentType() + ";base64, " + i.getData());
    }

    private String convertLineBreaksToParagraphs(EmailContent content) {
        return Stream.of(content.getData().split("\n")).map(v -> "<p>" + v + "</p>").collect(Collectors.joining());
    }
}
