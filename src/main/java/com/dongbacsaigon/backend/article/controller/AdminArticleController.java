package com.dongbacsaigon.backend.article.controller;

import java.util.List;
import java.util.UUID;

import com.dongbacsaigon.backend.article.dto.AdminArticlePageResponse;
import com.dongbacsaigon.backend.article.dto.AdminArticleResponse;
import com.dongbacsaigon.backend.article.dto.ArticleDraftRequest;
import com.dongbacsaigon.backend.article.dto.ArticleRevisionResponse;
import com.dongbacsaigon.backend.article.dto.ArticleRevisionSummaryResponse;
import com.dongbacsaigon.backend.article.entity.ArticlePublicationStatus;
import com.dongbacsaigon.backend.article.entity.ArticleRevisionStatus;
import com.dongbacsaigon.backend.article.entity.ArticleType;
import com.dongbacsaigon.backend.article.service.ArticleService;
import com.dongbacsaigon.backend.auth.security.AuthenticatedUserProvider;
import com.dongbacsaigon.backend.common.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/articles")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Articles and Revisions")
class AdminArticleController {

    private final ArticleService service;
    private final AuthenticatedUserProvider userProvider;

    AdminArticleController(ArticleService service, AuthenticatedUserProvider userProvider) {
        this.service = service;
        this.userProvider = userProvider;
    }

    @GetMapping
    @Operation(summary = "List Articles", description = "ADMIN and STAFF. Search title, slug and summary with bounded pagination.")
    AdminArticlePageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ArticleType articleType,
            @RequestParam(required = false) ArticlePublicationStatus publicationStatus,
            @RequestParam(required = false) ArticleRevisionStatus revisionStatus
    ) { return service.list(page, size, search, articleType, publicationStatus, revisionStatus); }

    @GetMapping("/{articleId}")
    @Operation(summary = "Get Article aggregate")
    AdminArticleResponse get(@PathVariable UUID articleId) { return service.get(articleId); }

    @PostMapping
    @Operation(summary = "Create Article draft", description = "Creates stable Article identity plus Revision 1 DRAFT.")
    ArticleRevisionResponse create(@Valid @RequestBody ArticleDraftRequest request, Authentication authentication) {
        return service.create(request, userProvider.requireUserId(authentication));
    }

    @PostMapping("/{articleId}/revisions")
    @Operation(summary = "Create next Article revision", description = "Clones latest rejected revision, otherwise current published revision.")
    ArticleRevisionResponse createRevision(@PathVariable UUID articleId, Authentication authentication) { return service.createRevision(articleId, userProvider.requireUserId(authentication)); }

    @GetMapping("/{articleId}/revisions")
    @Operation(summary = "List Article revision history")
    List<ArticleRevisionSummaryResponse> listRevisions(@PathVariable UUID articleId) { return service.listRevisions(articleId); }

    @GetMapping("/{articleId}/revisions/{revisionId}")
    @Operation(summary = "Get Article revision")
    ArticleRevisionResponse getRevision(@PathVariable UUID articleId, @PathVariable UUID revisionId) { return service.getRevision(articleId, revisionId); }

    @PatchMapping("/{articleId}/revisions/{revisionId}")
    @Operation(summary = "Replace Article draft content", description = "Only DRAFT is mutable. HTML content is sanitized server-side.")
    ArticleRevisionResponse updateRevision(@PathVariable UUID articleId, @PathVariable UUID revisionId, @Valid @RequestBody ArticleDraftRequest request, Authentication authentication) {
        return service.updateDraft(articleId, revisionId, request, userProvider.requireUserId(authentication));
    }

    @PostMapping("/{articleId}/revisions/{revisionId}/submit")
    @Operation(summary = "Submit Article to Approval Center", description = "Uses ARTICLE, stable Article ID and revision number.")
    ArticleRevisionResponse submit(@PathVariable UUID articleId, @PathVariable UUID revisionId, Authentication authentication) {
        return service.submit(articleId, revisionId, userProvider.requireUserId(authentication));
    }

    @DeleteMapping("/{articleId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete never-submitted draft Article", description = "ADMIN only. Blocked after submission or publication.")
    MessageResponse delete(@PathVariable UUID articleId, Authentication authentication) {
        service.deleteDraftArticle(articleId, userProvider.requireUserId(authentication));
        return new MessageResponse("Draft article deleted.");
    }

    @PostMapping("/{articleId}/unpublish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unpublish Article", description = "ADMIN only.")
    AdminArticleResponse unpublish(@PathVariable UUID articleId, Authentication authentication) { return service.unpublish(articleId, userProvider.requireUserId(authentication)); }

    @PostMapping("/{articleId}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Republish unchanged approved Article", description = "ADMIN only. Never publishes a DRAFT.")
    AdminArticleResponse republish(@PathVariable UUID articleId, Authentication authentication) { return service.republish(articleId, userProvider.requireUserId(authentication)); }

    @PostMapping("/{articleId}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Archive Article", description = "ADMIN only and terminal in Phase 6.")
    AdminArticleResponse archive(@PathVariable UUID articleId, Authentication authentication) { return service.archive(articleId, userProvider.requireUserId(authentication)); }
}
