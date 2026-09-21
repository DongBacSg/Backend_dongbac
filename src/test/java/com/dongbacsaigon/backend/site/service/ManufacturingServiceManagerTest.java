package com.dongbacsaigon.backend.site.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.article.service.ArticleContentSanitizer;
import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.catalog.service.SlugService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.media.entity.Media;
import com.dongbacsaigon.backend.media.entity.MediaType;
import com.dongbacsaigon.backend.media.service.MediaService;
import com.dongbacsaigon.backend.site.dto.ManufacturingServiceRequest;
import com.dongbacsaigon.backend.site.entity.ManufacturingService;
import com.dongbacsaigon.backend.site.repository.ManufacturingServiceRepository;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

class ManufacturingServiceManagerTest {

    private final ManufacturingServiceRepository repository = mock(ManufacturingServiceRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final MediaService mediaService = mock(MediaService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final ManufacturingServiceManager manager = new ManufacturingServiceManager(
            repository, userRepository, mediaService, new SlugService(), new ArticleContentSanitizer(), auditService
    );

    @Test
    void adminCreatesServiceWithActiveImageSafeContentAndAudit() {
        User admin = admin();
        Media image = image(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(mediaService.requireActiveImage(image.getId())).thenReturn(image);
        when(repository.save(any(ManufacturingService.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = manager.create(new ManufacturingServiceRequest(
                " Gia cong phan bon NPK ", null, " <b>Mo ta</b> ",
                "<p>Noi dung</p><script>alert(1)</script>", image.getId(), null, null, 2, true
        ), admin.getId());

        ArgumentCaptor<ManufacturingService> saved = ArgumentCaptor.forClass(ManufacturingService.class);
        verify(repository).save(saved.capture());
        assertThat(response.slug()).isEqualTo("gia-cong-phan-bon-npk");
        assertThat(response.summary()).isEqualTo("Mo ta");
        assertThat(response.content()).contains("<p>Noi dung</p>").doesNotContain("script");
        assertThat(response.featuredImage().id()).isEqualTo(image.getId());
        assertThat(saved.getValue().isActive()).isTrue();
        verify(auditService).recordSuccessAfterCommit(admin, AuditAction.MANUFACTURING_SERVICE_CREATED,
                AuditTargetType.MANUFACTURING_SERVICE, response.id(), null);
    }

    @Test
    void duplicateSlugIsRejected() {
        User admin = admin();
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(repository.existsBySlugAndIdNot(any(String.class), any(UUID.class))).thenReturn(true);

        assertThatThrownBy(() -> manager.create(request("Existing", null), admin.getId()))
                .isInstanceOf(ApiException.class).hasMessage("Manufacturing service slug is already in use.");
        verify(repository, never()).save(any(ManufacturingService.class));
    }

    @Test
    void invalidImageIsRejectedByMediaLibrary() {
        User admin = admin();
        UUID mediaId = UUID.randomUUID();
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(mediaService.requireActiveImage(mediaId))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "Media must be an active image."));

        assertThatThrownBy(() -> manager.create(request("Service", mediaId), admin.getId()))
                .isInstanceOf(ApiException.class).hasMessage("Media must be an active image.");
        verify(repository, never()).save(any(ManufacturingService.class));
    }

    @Test
    void publicListUsesActiveOnlyQueryAndOmitsContent() {
        ManufacturingService service = service(true);
        when(repository.findByActiveTrueOrderBySortOrderAscTitleAscIdAsc()).thenReturn(List.of(service));

        var result = manager.listPublic();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).slug()).isEqualTo("service");
        assertThat(result.get(0).getClass().getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName).doesNotContain("content");
    }

    @Test
    void publicDetailResolvesOnlyActiveSlug() {
        ManufacturingService service = service(true);
        when(repository.findBySlugAndActiveTrue("service")).thenReturn(Optional.of(service));

        assertThat(manager.getPublic("service").content()).isEqualTo("<p>Content</p>");
        assertThatThrownBy(() -> manager.getPublic("inactive"))
                .isInstanceOf(ApiException.class).hasMessage("Manufacturing service not found.");
        verify(repository).findBySlugAndActiveTrue("inactive");
    }

    @Test
    void referencedFeaturedMediaIsProtectedByExistingCheckerChain() {
        UUID mediaId = UUID.randomUUID();
        when(repository.existsByFeaturedMediaId(mediaId)).thenReturn(true);

        assertThat(new ManufacturingServiceMediaReferenceChecker(repository).isReferenced(mediaId)).isTrue();
    }

    private ManufacturingServiceRequest request(String title, UUID mediaId) {
        return new ManufacturingServiceRequest(title, null, null, null, mediaId, null, null, null, null);
    }

    private ManufacturingService service(boolean active) {
        return new ManufacturingService("service", "Service", "Summary", "<p>Content</p>",
                null, null, null, 0, active);
    }

    private User admin() {
        return User.admin("admin@example.com", "$2a$12$hash", "Admin");
    }

    private Media image(User uploader) {
        return new Media("asset-image", "dongbac/media/image", MediaType.IMAGE, "webp",
                "https://res.cloudinary.com/demo/image/upload/v1/image.webp", 800, 600, 300_000,
                BigDecimal.ZERO, "dongbac/media", "image.webp", uploader);
    }
}
