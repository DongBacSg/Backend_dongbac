package com.dongbacsaigon.backend.lead.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.UUID;

import com.dongbacsaigon.backend.lead.entity.LeadStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

class CustomerLeadRepositoryContractTest {

    @Test
    void listQuerySearchesRequiredFieldsAndExcludesSensitiveNotes() throws Exception {
        Method method = CustomerLeadRepository.class.getDeclaredMethod(
                "findAdminPage", String.class, LeadStatus.class, UUID.class, boolean.class, Pageable.class
        );
        String query = method.getAnnotation(Query.class).value();

        assertThat(query)
                .contains("lead.fullName", "lead.phone", "lead.email", "lead.companyName")
                .contains("lead.status", "lead.assignedTo")
                .doesNotContain("internalNote", "message");
        assertThat(method.getAnnotation(EntityGraph.class).attributePaths())
                .containsExactlyInAnyOrder("assignedTo", "createdBy");
    }

    @Test
    void dashboardLeadCountsUseDatabaseGrouping() throws Exception {
        Query query = CustomerLeadRepository.class.getDeclaredMethod("countGroupedByStatus").getAnnotation(Query.class);
        assertThat(query.value().toLowerCase()).contains("count(lead)", "group by lead.status");
    }
}
