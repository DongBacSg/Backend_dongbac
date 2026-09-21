package com.dongbacsaigon.backend.lead.dto;

import com.dongbacsaigon.backend.lead.entity.LeadStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateLeadStatusRequest(@NotNull LeadStatus status) {
}
