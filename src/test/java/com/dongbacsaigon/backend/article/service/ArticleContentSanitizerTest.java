package com.dongbacsaigon.backend.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dongbacsaigon.backend.common.exception.ApiException;
import org.junit.jupiter.api.Test;

class ArticleContentSanitizerTest {

    private final ArticleContentSanitizer sanitizer = new ArticleContentSanitizer();

    @Test
    void removesScriptsEventHandlersAndUnsafeProtocols() {
        String result = sanitizer.sanitizeRequired(
                "<h2>Title</h2><script>alert(1)</script><p onclick='x()'>Body</p><a href='javascript:alert(1)'>Link</a>"
        );

        assertThat(result).contains("<h2>Title</h2>", "<p>Body</p>", ">Link</a>");
        assertThat(result).doesNotContain("script", "onclick", "javascript:");
    }

    @Test
    void rejectsContentWithoutReadableText() {
        assertThatThrownBy(() -> sanitizer.sanitizeRequired("<script>alert(1)</script>"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Article content must contain readable text.");
    }
}
