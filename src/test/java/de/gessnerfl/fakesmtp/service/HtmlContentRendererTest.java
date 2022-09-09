package de.gessnerfl.fakesmtp.service;

import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailContent;
import de.gessnerfl.fakesmtp.model.InlineImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationContext;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HtmlContentRendererTest {

    private BuildProperties buildProperties;
    private HtmlContentRenderer sut;

    @BeforeEach
    void init(){
        var applicationContext = mock(ApplicationContext.class);
        buildProperties = mock(BuildProperties.class);
        when(applicationContext.getBean(BuildProperties.class)).thenReturn(buildProperties);

        sut = new HtmlContentRenderer(applicationContext);
    }

    @Test
    void shouldReturnContentAsIsWhenContentTypeIsNotHtmlAndNotPlain(){
        var data = "content";
        var content = mock(EmailContent.class);
        when(content.getData()).thenReturn(data);

        var result = sut.render(content);

        assertEquals(data, result);
    }

    @Test
    void shouldAddHeaderAndBodyTagsWhenMissing(){
        var html = "<div>Test</div>";
        var content = mock(EmailContent.class);
        when(content.getData()).thenReturn(html);
        when(content.getContentType()).thenReturn(ContentType.HTML);

        var result = sut.render(content);

        assertEquals("<html>\n <head></head>\n <body>\n  <div>\n   Test\n  </div>\n </body>\n</html>", result);
    }

    @Test
    void shouldAddBootstrapCssWhenAvailable(){
        var bootstrapVersion = "version";
        var html = "<div>Test</div>";
        var content = mock(EmailContent.class);
        when(content.getData()).thenReturn(html);
        when(content.getContentType()).thenReturn(ContentType.HTML);
        when(buildProperties.get(HtmlContentRenderer.BOOTSTRAP_VERSION)).thenReturn(bootstrapVersion);

        var result = sut.render(content);

        assertEquals("<html>\n <head>\n  <link rel=\"stylesheet\" href=\"/webjars/bootstrap/version/css/bootstrap.min.css\">\n </head>\n <body>\n  <div>\n   Test\n  </div>\n </body>\n</html>", result);
    }

    @Test
    void shouldReplaceInlineImageWhenImageIsAvailable(){
        var html = "<img alt=\"test image\" src=\"cid:test\">";
        var content = mock(EmailContent.class);
        var email = mock(Email.class);
        var inlineImage = mock(InlineImage.class);
        when(content.getData()).thenReturn(html);
        when(content.getContentType()).thenReturn(ContentType.HTML);
        when(content.getEmail()).thenReturn(email);
        when(email.getInlineImageByContentId("test")).thenReturn(Optional.of(inlineImage));
        when(inlineImage.getContentType()).thenReturn("image/png");
        when(inlineImage.getData()).thenReturn("image_data");

        var result = sut.render(content);

        assertEquals("<html>\n <head></head>\n <body>\n  <img alt=\"test image\" src=\"data:image/png;base64, image_data\">\n </body>\n</html>", result);
    }

    @Test
    void shouldKeepInlineImagePlaceholderWhenInlineImageIsNotAvailable(){
        var html = "<img alt=\"test image\" src=\"cid:test\">";
        var content = mock(EmailContent.class);
        var email = mock(Email.class);
        when(content.getData()).thenReturn(html);
        when(content.getContentType()).thenReturn(ContentType.HTML);
        when(content.getEmail()).thenReturn(email);
        when(email.getInlineImageByContentId("test")).thenReturn(Optional.empty());

        var result = sut.render(content);

        assertEquals("<html>\n <head></head>\n <body>\n  <img alt=\"test image\" src=\"cid:test\">\n </body>\n</html>", result);
    }

    @Test
    void shouldReturnConvertLinebreaksToParagraphsForPlainContentType(){
        var data = "content1\ncontent2";
        var content = mock(EmailContent.class);
        when(content.getData()).thenReturn(data);
        when(content.getContentType()).thenReturn(ContentType.PLAIN);

        var result = sut.render(content);

        assertEquals("<p>content1</p><p>content2</p>", result);
    }

}