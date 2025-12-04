package com.irembo.certify.tenant;

import com.irembo.certify.tenant.dto.CreateTenantRequest;
import com.irembo.certify.tenant.dto.TenantSummaryResponse;
import com.irembo.certify.user.Role;
import com.irembo.certify.user.User;
import com.irembo.certify.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantAdminServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private TenantAdminService tenantAdminService;

    @BeforeEach
    void setUp() {
        tenantAdminService = new TenantAdminService(tenantRepository, userRepository, passwordEncoder);
    }

    @Test
    void createTenantCreatesTenantAndAdmin() {
        CreateTenantRequest request = new CreateTenantRequest(
                "Acme Corp",
                null,
                "Alice Admin",
                "alice@example.com",
                "  passw0rd!  "
        );

        when(tenantRepository.existsBySlug("acme-corp")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("passw0rd!")).thenReturn("hashed");
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant tenant = invocation.getArgument(0);
            tenant.setId(UUID.randomUUID());
            tenant.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
            tenant.setUpdatedAt(Instant.parse("2024-01-01T00:00:00Z"));
            return tenant;
        });
        TenantSummaryResponse response = tenantAdminService.createTenant(request);

        assertThat(response.name()).isEqualTo("Acme Corp");
        assertThat(response.slug()).isEqualTo("acme-corp");
        assertThat(response.admin()).isNotNull();
        assertThat(response.admin().email()).isEqualTo("alice@example.com");

        ArgumentCaptor<User> adminCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(adminCaptor.capture());
        User savedAdmin = adminCaptor.getValue();
        assertThat(savedAdmin.getFullName()).isEqualTo("Alice Admin");
        assertThat(savedAdmin.getRole()).isEqualTo(Role.TENANT_ADMIN);
        assertThat(savedAdmin.getPasswordHash()).isEqualTo("hashed");
        verify(passwordEncoder).encode("passw0rd!");
    }

    @Test
    void createTenantRejectsDuplicateSlug() {
        CreateTenantRequest request = new CreateTenantRequest(
                "Existing",
                null,
                "Eve Example",
                "eve@example.com",
                "secret123"
        );

        when(tenantRepository.existsBySlug("existing")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> tenantAdminService.createTenant(request));
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void listTenantsHandlesMissingAdmin() {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName("No Admin Corp");
        tenant.setSlug("no-admin-corp");
        tenant.setCreatedAt(Instant.now());
        tenant.setUpdatedAt(Instant.now());

        when(tenantRepository.findAll(any(Sort.class))).thenReturn(java.util.List.of(tenant));
        when(userRepository.findByTenantIdInAndRoleOrderByCreatedAtAsc(anyList(), eq(Role.TENANT_ADMIN)))
                .thenReturn(List.of());

        java.util.List<TenantSummaryResponse> responses = tenantAdminService.listTenants();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).admin()).isNotNull();
        assertThat(responses.get(0).admin().fullName()).isEmpty();
        assertThat(responses.get(0).admin().email()).isEmpty();

        verify(userRepository, times(1)).findByTenantIdInAndRoleOrderByCreatedAtAsc(anyList(), eq(Role.TENANT_ADMIN));
    }
}
