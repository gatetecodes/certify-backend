package com.irembo.certify.tenant;

import com.irembo.certify.tenant.dto.CreateTenantRequest;
import com.irembo.certify.tenant.dto.TenantSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tenants")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class TenantAdminController {

    private final TenantAdminService tenantAdminService;

    public TenantAdminController(TenantAdminService tenantAdminService) {
        this.tenantAdminService = tenantAdminService;
    }

    @GetMapping
    public List<TenantSummaryResponse> listTenants() {
        return tenantAdminService.listTenants();
    }

    @PostMapping
    public TenantSummaryResponse createTenant(@Valid @RequestBody CreateTenantRequest request) {
        return tenantAdminService.createTenant(request);
    }
}
