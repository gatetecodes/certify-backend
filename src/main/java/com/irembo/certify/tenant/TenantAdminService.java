package com.irembo.certify.tenant;

import com.irembo.certify.tenant.dto.CreateTenantRequest;
import com.irembo.certify.tenant.dto.TenantSummaryResponse;
import com.irembo.certify.user.Role;
import com.irembo.certify.user.User;
import com.irembo.certify.user.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@Service
public class TenantAdminService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantAdminService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<TenantSummaryResponse> listTenants() {
        List<Tenant> tenants = tenantRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt")).stream()
                .filter(tenant -> !"platform".equals(tenant.getSlug()))
                .toList();

        if (tenants.isEmpty()) {
            return List.of();
        }

        List<UUID> tenantIds = tenants.stream()
                .map(Tenant::getId)
                .toList();

        // Batch load all tenant admins in a single query to avoid N+1 lookups
        List<User> admins = userRepository.findByTenantIdInAndRoleOrderByCreatedAtAsc(tenantIds, Role.TENANT_ADMIN);

        Map<UUID, TenantSummaryResponse.TenantAdminContact> adminContactsByTenantId = new HashMap<>();
        for (User admin : admins) {
            // Because the result is ordered by createdAt asc, the first admin we see per tenant
            // is the earliest one; keep the existing entry if already present.
            adminContactsByTenantId.computeIfAbsent(
                    admin.getTenantId(),
                    id -> new TenantSummaryResponse.TenantAdminContact(admin.getFullName(), admin.getEmail())
            );
        }

        return tenants.stream()
                .map(tenant -> new TenantSummaryResponse(
                        tenant.getId(),
                        tenant.getName(),
                        tenant.getSlug(),
                        tenant.getCreatedAt(),
                        tenant.getUpdatedAt(),
                        adminContactsByTenantId.getOrDefault(
                                tenant.getId(),
                                new TenantSummaryResponse.TenantAdminContact("", "")
                        )
                ))
                .toList();
    }

    @Transactional
    public TenantSummaryResponse createTenant(CreateTenantRequest request) {
        String tenantName = request.tenantName().trim();
        String slug = determineSlug(tenantName, request.tenantSlug());

        if (tenantRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Tenant slug already exists: " + slug);
        }

        String adminEmail = request.adminEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(adminEmail)) {
            throw new IllegalArgumentException("Admin email already in use: " + adminEmail);
        }

        Tenant tenant = new Tenant();
        tenant.setName(tenantName);
        tenant.setSlug(slug);
        Tenant savedTenant = tenantRepository.save(tenant);

        String rawPassword = request.adminPassword();
        String trimmedPassword = rawPassword == null ? null : rawPassword.trim();

        User admin = new User();
        admin.setTenantId(savedTenant.getId());
        admin.setFullName(request.adminFullName().trim());
        admin.setEmail(adminEmail);
        admin.setRole(Role.TENANT_ADMIN);
        admin.setPasswordHash(passwordEncoder.encode(trimmedPassword));
        userRepository.save(admin);

        TenantSummaryResponse.TenantAdminContact contact =
                new TenantSummaryResponse.TenantAdminContact(admin.getFullName(), admin.getEmail());

        return new TenantSummaryResponse(
                savedTenant.getId(),
                savedTenant.getName(),
                savedTenant.getSlug(),
                savedTenant.getCreatedAt(),
                savedTenant.getUpdatedAt(),
                contact
        );
    }

    private String determineSlug(String tenantName, String requestedSlug) {
        String base = StringUtils.hasText(requestedSlug) ? requestedSlug : tenantName;
        String normalized = Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = normalized
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (!StringUtils.hasText(slug)) {
            throw new IllegalArgumentException("Unable to derive tenant slug from input");
        }
        return slug;
    }
}
