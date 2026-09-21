package com.dongbacsaigon.backend.lead.service;

import com.dongbacsaigon.backend.catalog.service.CatalogMapper;
import com.dongbacsaigon.backend.lead.dto.LeadResponse;
import com.dongbacsaigon.backend.lead.dto.LeadSummaryResponse;
import com.dongbacsaigon.backend.lead.entity.CustomerLead;

public final class LeadMapper {

    private LeadMapper() {
    }

    public static LeadSummaryResponse toSummary(CustomerLead lead) {
        return new LeadSummaryResponse(
                lead.getId(),
                lead.getFullName(),
                lead.getPhone(),
                lead.getEmail(),
                lead.getCompanyName(),
                lead.getStatus(),
                CatalogMapper.toUserSummary(lead.getAssignedTo()),
                CatalogMapper.toUserSummary(lead.getCreatedBy()),
                lead.getCreatedAt(),
                lead.getUpdatedAt()
        );
    }

    public static LeadResponse toResponse(CustomerLead lead) {
        return new LeadResponse(
                lead.getId(),
                lead.getFullName(),
                lead.getPhone(),
                lead.getEmail(),
                lead.getCompanyName(),
                lead.getMessage(),
                lead.getStatus(),
                CatalogMapper.toUserSummary(lead.getAssignedTo()),
                lead.getInternalNote(),
                CatalogMapper.toUserSummary(lead.getCreatedBy()),
                lead.getCreatedAt(),
                lead.getUpdatedAt()
        );
    }
}
