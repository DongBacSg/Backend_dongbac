package com.dongbacsaigon.backend.dashboard.service;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import com.dongbacsaigon.backend.approval.entity.ApprovalRequestStatus;
import com.dongbacsaigon.backend.approval.repository.ApprovalRequestRepository;
import com.dongbacsaigon.backend.article.entity.ArticlePublicationStatus;
import com.dongbacsaigon.backend.article.repository.ArticleRepository;
import com.dongbacsaigon.backend.catalog.entity.ProductPublicationStatus;
import com.dongbacsaigon.backend.catalog.repository.ProductRepository;
import com.dongbacsaigon.backend.dashboard.dto.DashboardSummaryResponse;
import com.dongbacsaigon.backend.lead.entity.LeadStatus;
import com.dongbacsaigon.backend.lead.repository.CustomerLeadRepository;
import com.dongbacsaigon.backend.media.entity.MediaStatus;
import com.dongbacsaigon.backend.media.repository.MediaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final CustomerLeadRepository leadRepository;
    private final ApprovalRequestRepository approvalRepository;
    private final ProductRepository productRepository;
    private final ArticleRepository articleRepository;
    private final MediaRepository mediaRepository;

    public DashboardService(
            CustomerLeadRepository leadRepository,
            ApprovalRequestRepository approvalRepository,
            ProductRepository productRepository,
            ArticleRepository articleRepository,
            MediaRepository mediaRepository
    ) {
        this.leadRepository = leadRepository;
        this.approvalRepository = approvalRepository;
        this.productRepository = productRepository;
        this.articleRepository = articleRepository;
        this.mediaRepository = mediaRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(UUID currentUserId, boolean admin) {
        return new DashboardSummaryResponse(
                leadCounts(),
                new DashboardSummaryResponse.ApprovalCounts(admin
                        ? approvalRepository.countByStatus(ApprovalRequestStatus.PENDING)
                        : approvalRepository.countByStatusAndSubmittedById(ApprovalRequestStatus.PENDING, currentUserId)),
                productCounts(),
                articleCounts(),
                new DashboardSummaryResponse.MediaCounts(mediaRepository.countByStatus(MediaStatus.ACTIVE))
        );
    }

    private DashboardSummaryResponse.LeadCounts leadCounts() {
        Map<LeadStatus, Long> counts = new EnumMap<>(LeadStatus.class);
        leadRepository.countGroupedByStatus().forEach(row -> counts.put(row.getStatus(), row.getCount()));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return new DashboardSummaryResponse.LeadCounts(
                total,
                counts.getOrDefault(LeadStatus.NEW, 0L),
                counts.getOrDefault(LeadStatus.CONTACTED, 0L),
                counts.getOrDefault(LeadStatus.IN_PROGRESS, 0L),
                counts.getOrDefault(LeadStatus.COMPLETED, 0L),
                counts.getOrDefault(LeadStatus.CANCELLED, 0L),
                counts.getOrDefault(LeadStatus.SPAM, 0L)
        );
    }

    private DashboardSummaryResponse.PublicationCounts productCounts() {
        Map<ProductPublicationStatus, Long> counts = new EnumMap<>(ProductPublicationStatus.class);
        productRepository.countGroupedByPublicationStatus().forEach(row -> counts.put(row.getStatus(), row.getCount()));
        long draft = counts.getOrDefault(ProductPublicationStatus.DRAFT, 0L);
        long published = counts.getOrDefault(ProductPublicationStatus.PUBLISHED, 0L);
        long unpublished = counts.getOrDefault(ProductPublicationStatus.UNPUBLISHED, 0L);
        long archived = counts.getOrDefault(ProductPublicationStatus.ARCHIVED, 0L);
        return new DashboardSummaryResponse.PublicationCounts(
                draft + published + unpublished + archived, published, draft, unpublished, archived
        );
    }

    private DashboardSummaryResponse.PublicationCounts articleCounts() {
        Map<ArticlePublicationStatus, Long> counts = new EnumMap<>(ArticlePublicationStatus.class);
        articleRepository.countGroupedByPublicationStatus().forEach(row -> counts.put(row.getStatus(), row.getCount()));
        long draft = counts.getOrDefault(ArticlePublicationStatus.DRAFT, 0L);
        long published = counts.getOrDefault(ArticlePublicationStatus.PUBLISHED, 0L);
        long unpublished = counts.getOrDefault(ArticlePublicationStatus.UNPUBLISHED, 0L);
        long archived = counts.getOrDefault(ArticlePublicationStatus.ARCHIVED, 0L);
        return new DashboardSummaryResponse.PublicationCounts(
                draft + published + unpublished + archived, published, draft, unpublished, archived
        );
    }
}
