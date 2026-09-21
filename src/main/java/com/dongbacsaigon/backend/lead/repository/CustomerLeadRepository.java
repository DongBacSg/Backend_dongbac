package com.dongbacsaigon.backend.lead.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.lead.entity.CustomerLead;
import com.dongbacsaigon.backend.lead.entity.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerLeadRepository extends JpaRepository<CustomerLead, UUID> {

    @EntityGraph(attributePaths = {"assignedTo", "createdBy"})
    Optional<CustomerLead> findDetailedById(UUID id);

    @EntityGraph(attributePaths = {"assignedTo", "createdBy"})
    @Query("""
            select lead from CustomerLead lead
            where (:status is null or lead.status = :status)
              and (:assignedTo is null or lead.assignedTo.id = :assignedTo)
              and (:unassigned = false or lead.assignedTo is null)
              and (:search is null
                   or lower(lead.fullName) like concat('%', :search, '%')
                   or lower(lead.phone) like concat('%', :search, '%')
                   or lower(lead.email) like concat('%', :search, '%')
                   or lower(lead.companyName) like concat('%', :search, '%'))
            """)
    Page<CustomerLead> findAdminPage(
            @Param("search") String search,
            @Param("status") LeadStatus status,
            @Param("assignedTo") UUID assignedTo,
            @Param("unassigned") boolean unassigned,
            Pageable pageable
    );

    @Query("select lead.status as status, count(lead) as count from CustomerLead lead group by lead.status")
    List<LeadStatusCount> countGroupedByStatus();
}
