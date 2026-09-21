package com.dongbacsaigon.backend.article.controller;

import com.dongbacsaigon.backend.article.dto.PublicArticlePageResponse;
import com.dongbacsaigon.backend.article.dto.PublicArticleResponse;
import com.dongbacsaigon.backend.article.entity.ArticleType;
import com.dongbacsaigon.backend.article.service.PublicArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/articles")
@Tag(name = "Public Articles")
class PublicArticleController {

    private final PublicArticleService service;

    PublicArticleController(PublicArticleService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "List public Articles", description = "Only current PUBLISHED revisions. Types: INTERNAL_ACTIVITY, NEWS, KNOWLEDGE.")
    PublicArticlePageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ArticleType type,
            @RequestParam(required = false) String search
    ) { return service.list(page, size, type, search); }

    @GetMapping("/internal-activities")
    @Operation(summary = "List public internal activities")
    PublicArticlePageResponse internalActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search
    ) { return service.list(page, size, ArticleType.INTERNAL_ACTIVITY, search); }

    @GetMapping("/{slug}")
    @Operation(summary = "Get public Article by current published slug")
    PublicArticleResponse get(@PathVariable String slug) { return service.getBySlug(slug); }
}
