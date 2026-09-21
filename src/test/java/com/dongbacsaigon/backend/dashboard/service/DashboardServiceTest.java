package com.dongbacsaigon.backend.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import com.dongbacsaigon.backend.approval.entity.ApprovalRequestStatus;
import com.dongbacsaigon.backend.approval.repository.ApprovalRequestRepository;
import com.dongbacsaigon.backend.article.entity.ArticlePublicationStatus;
import com.dongbacsaigon.backend.article.repository.ArticlePublicationStatusCount;
import com.dongbacsaigon.backend.article.repository.ArticleRepository;
import com.dongbacsaigon.backend.catalog.entity.ProductPublicationStatus;
import com.dongbacsaigon.backend.catalog.repository.ProductPublicationStatusCount;
import com.dongbacsaigon.backend.catalog.repository.ProductRepository;
import com.dongbacsaigon.backend.dashboard.dto.DashboardSummaryResponse;
import com.dongbacsaigon.backend.lead.entity.LeadStatus;
import com.dongbacsaigon.backend.lead.repository.CustomerLeadRepository;
import com.dongbacsaigon.backend.lead.repository.LeadStatusCount;
import com.dongbacsaigon.backend.media.entity.MediaStatus;
import com.dongbacsaigon.backend.media.repository.MediaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class DashboardServiceTest {

    private final CustomerLeadRepository leadRepository = mock(CustomerLeadRepository.class);
    private final ApprovalRequestRepository approvalRepository = mock(ApprovalRequestRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ArticleRepository articleRepository = mock(ArticleRepository.class);
    private final MediaRepository mediaRepository = mock(MediaRepository.class);
    private final DashboardService service = new DashboardService(
            leadRepository, approvalRepository, productRepository, articleRepository, mediaRepository
    );

    @BeforeEach
    void stubDomainCounts() {
        LeadStatusCount newCount = count(LeadStatus.NEW, 2);
        LeadStatusCount contactedCount = count(LeadStatus.CONTACTED, 3);
        LeadStatusCount completedCount = count(LeadStatus.COMPLETED, 1);
        when(leadRepository.countGroupedByStatus()).thenReturn(List.of(newCount, contactedCount, completedCount));
        ProductPublicationStatusCount productDraft = productCount(ProductPublicationStatus.DRAFT, 2);
        ProductPublicationStatusCount productPublished = productCount(ProductPublicationStatus.PUBLISHED, 3);
        ProductPublicationStatusCount productUnpublished = productCount(ProductPublicationStatus.UNPUBLISHED, 1);
        ProductPublicationStatusCount productArchived = productCount(ProductPublicationStatus.ARCHIVED, 4);
        when(productRepository.countGroupedByPublicationStatus()).thenReturn(List.of(
                productDraft, productPublished, productUnpublished, productArchived
        ));
        ArticlePublicationStatusCount articleDraft = articleCount(ArticlePublicationStatus.DRAFT, 5);
        ArticlePublicationStatusCount articlePublished = articleCount(ArticlePublicationStatus.PUBLISHED, 2);
        ArticlePublicationStatusCount articleUnpublished = articleCount(ArticlePublicationStatus.UNPUBLISHED, 1);
        ArticlePublicationStatusCount articleArchived = articleCount(ArticlePublicationStatus.ARCHIVED, 1);
        when(articleRepository.countGroupedByPublicationStatus()).thenReturn(List.of(
                articleDraft, articlePublished, articleUnpublished, articleArchived
        ));
        when(mediaRepository.countByStatus(MediaStatus.ACTIVE)).thenReturn(8L);
    }

    @Test
    void adminSummaryUsesRealIdentityCountsAndGlobalPendingApprovals() {
        UUID adminId = UUID.randomUUID();
        when(approvalRepository.countByStatus(ApprovalRequestStatus.PENDING)).thenReturn(7L);

        DashboardSummaryResponse response = service.summary(adminId, true);

        assertThat(response.leads().total()).isEqualTo(6);
        assertThat(response.leads().newCount()).isEqualTo(2);
        assertThat(response.leads().inProgress()).isZero();
        assertThat(response.approvals().pending()).isEqualTo(7);
        assertThat(response.products().total()).isEqualTo(10);
        assertThat(response.products().published()).isEqualTo(3);
        assertThat(response.articles().total()).isEqualTo(9);
        assertThat(response.media().active()).isEqualTo(8);
        verify(approvalRepository).countByStatus(ApprovalRequestStatus.PENDING);
        verify(approvalRepository, never()).countByStatusAndSubmittedById(ApprovalRequestStatus.PENDING, adminId);
    }

    @Test
    void staffSummaryCountsOnlyPendingApprovalsSubmittedByThatStaffUser() {
        UUID staffId = UUID.randomUUID();
        when(approvalRepository.countByStatusAndSubmittedById(ApprovalRequestStatus.PENDING, staffId)).thenReturn(2L);

        DashboardSummaryResponse response = service.summary(staffId, false);

        assertThat(response.approvals().pending()).isEqualTo(2);
        verify(approvalRepository).countByStatusAndSubmittedById(ApprovalRequestStatus.PENDING, staffId);
        verify(approvalRepository, never()).countByStatus(ApprovalRequestStatus.PENDING);
    }

    @Test
    void productAndArticleCountsGroupStableIdentityEntitiesInsteadOfRevisions() throws Exception {
        Query productQuery = ProductRepository.class.getDeclaredMethod("countGroupedByPublicationStatus").getAnnotation(Query.class);
        Query articleQuery = ArticleRepository.class.getDeclaredMethod("countGroupedByPublicationStatus").getAnnotation(Query.class);

        assertThat(productQuery.value()).contains("from Product product", "group by product.publicationStatus")
                .doesNotContain("ProductRevision");
        assertThat(articleQuery.value()).contains("from Article article", "group by article.publicationStatus")
                .doesNotContain("ArticleRevision");
    }

    private LeadStatusCount count(LeadStatus status, long count) {
        LeadStatusCount row = mock(LeadStatusCount.class);
        when(row.getStatus()).thenReturn(status);
        when(row.getCount()).thenReturn(count);
        return row;
    }

    private ProductPublicationStatusCount productCount(ProductPublicationStatus status, long count) {
        ProductPublicationStatusCount row = mock(ProductPublicationStatusCount.class);
        when(row.getStatus()).thenReturn(status);
        when(row.getCount()).thenReturn(count);
        return row;
    }

    private ArticlePublicationStatusCount articleCount(ArticlePublicationStatus status, long count) {
        ArticlePublicationStatusCount row = mock(ArticlePublicationStatusCount.class);
        when(row.getStatus()).thenReturn(status);
        when(row.getCount()).thenReturn(count);
        return row;
    }
}
