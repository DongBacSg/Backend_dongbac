package com.dongbacsaigon.backend.lead.repository;

import com.dongbacsaigon.backend.lead.entity.LeadStatus;

public interface LeadStatusCount {
    LeadStatus getStatus();
    long getCount();
}
