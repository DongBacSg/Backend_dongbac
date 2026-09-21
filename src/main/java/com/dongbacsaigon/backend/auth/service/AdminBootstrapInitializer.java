package com.dongbacsaigon.backend.auth.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
class AdminBootstrapInitializer implements ApplicationRunner {

    private final AdminBootstrapService adminBootstrapService;

    AdminBootstrapInitializer(AdminBootstrapService adminBootstrapService) {
        this.adminBootstrapService = adminBootstrapService;
    }

    @Override
    public void run(ApplicationArguments args) {
        adminBootstrapService.bootstrapIfNecessary();
    }
}
