package com.dongbacsaigon.backend.common.web;

import com.dongbacsaigon.backend.common.response.PingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Infrastructure")
class InfrastructureController {

    @GetMapping("/ping")
    @Operation(summary = "Infrastructure ping endpoint")
    PingResponse ping() {
        return new PingResponse("ok");
    }
}
