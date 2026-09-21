package com.dongbacsaigon.backend.article.service;

import com.dongbacsaigon.backend.common.exception.ApiException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ArticleContentSanitizer {

    private static final Safelist ALLOWED_HTML = new Safelist()
            .addTags("p", "br", "strong", "b", "em", "i", "u", "s", "ul", "ol", "li", "blockquote", "h2", "h3", "h4", "a")
            .addAttributes("a", "href", "title")
            .addProtocols("a", "href", "http", "https", "mailto");

    public String sanitizeRequired(String content) {
        if (!StringUtils.hasText(content)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Article content is required.");
        }
        String sanitized = Jsoup.clean(
                content.trim(),
                "",
                ALLOWED_HTML,
                new Document.OutputSettings().prettyPrint(false)
        ).trim();
        if (!StringUtils.hasText(Jsoup.parse(sanitized).text())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Article content must contain readable text.");
        }
        return sanitized;
    }
}
