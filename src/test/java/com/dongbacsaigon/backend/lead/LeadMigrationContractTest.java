package com.dongbacsaigon.backend.lead;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class LeadMigrationContractTest {

    @Test
    void v7CreatesOnlyCustomerLeadsWithExactStatusesAndNoSeedData() throws IOException {
        String sql = new ClassPathResource("db/migration/V7__create_customer_leads.sql")
                .getContentAsString(StandardCharsets.UTF_8)
                .toLowerCase();

        assertThat(sql)
                .contains("create table dongbac.customer_leads")
                .contains("'new', 'contacted', 'in_progress', 'completed', 'cancelled', 'spam'")
                .contains("foreign key (assigned_to) references dongbac.users")
                .contains("foreign key (created_by) references dongbac.users")
                .doesNotContain("create table dongbac.dashboard")
                .doesNotContain("create table dongbac.orders")
                .doesNotContain("create table dongbac.payments")
                .doesNotContain("create table dongbac.customers")
                .doesNotContain("insert into");
    }
}
