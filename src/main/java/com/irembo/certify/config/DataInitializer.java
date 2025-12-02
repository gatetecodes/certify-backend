package com.irembo.certify.config;

import com.irembo.certify.tenant.Tenant;
import com.irembo.certify.tenant.TenantRepository;
import com.irembo.certify.user.Role;
import com.irembo.certify.user.User;
import com.irembo.certify.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Configuration
@EnableConfigurationProperties(BootstrapAdminProperties.class)
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final BootstrapAdminProperties bootstrapProps;

    public DataInitializer(BootstrapAdminProperties bootstrapProps) {
        this.bootstrapProps = bootstrapProps;
    }

    @Bean
    CommandLineRunner seedInitialData(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (!bootstrapProps.enabled()) {
                log.info("Bootstrap admin disabled; skipping initial tenant/user creation.");
                return;
            }

            validateBootstrapConfiguration();

            // 1) Create platform tenant for system admin
            String platformTenantName = "Sec CERTIFICATE";
            String platformTenantSlug = "platform";

            Tenant platformTenant = tenantRepository.findBySlug(platformTenantSlug)
                    .orElseGet(() -> {
                        Tenant t = new Tenant();
                        t.setName(platformTenantName);
                        t.setSlug(platformTenantSlug);
                        return tenantRepository.save(t);
                    });

            // 2) Seed SYSTEM_ADMIN user attached to platform tenant
            User systemAdmin = userRepository.findByEmail(bootstrapProps.systemEmail())
                    .orElseGet(() -> {
                        User u = new User();
                        u.setTenantId(platformTenant.getId());
                        u.setEmail(bootstrapProps.systemEmail());
                        u.setFullName(bootstrapProps.systemFullName());
                        u.setRole(Role.SYSTEM_ADMIN);
                        u.setPasswordHash(passwordEncoder.encode(bootstrapProps.systemPassword()));
                        return userRepository.save(u);
                    });

            // 3) Seed first business tenant and TENANT_ADMIN user
            String tenantName = bootstrapProps.tenantName();
            String tenantSlug = bootstrapProps.tenantSlug() != null
                    ? bootstrapProps.tenantSlug()
                    : tenantName.toLowerCase(Locale.ROOT).replace(' ', '-');

            log.info("Bootstrapping SYSTEM_ADMIN '{}' and initial tenant '{}' (slug='{}') with admin '{}'",
                    bootstrapProps.systemEmail(), tenantName, tenantSlug, bootstrapProps.email());

            Tenant tenant = tenantRepository.findBySlug(tenantSlug)
                    .orElseGet(() -> {
                        Tenant t = new Tenant();
                        t.setName(tenantName);
                        t.setSlug(tenantSlug);
                        return tenantRepository.save(t);
                    });

            User admin = userRepository.findByEmail(bootstrapProps.email())
                    .orElseGet(() -> {
                        User u = new User();
                        u.setTenantId(tenant.getId());
                        u.setEmail(bootstrapProps.email());
                        u.setFullName(bootstrapProps.fullName());
                        u.setRole(Role.TENANT_ADMIN);
                        u.setPasswordHash(passwordEncoder.encode(bootstrapProps.password()));
                        return userRepository.save(u);
                    });

            log.info("Created platform tenant '{}' with system admin '{}'", platformTenant.getSlug(), systemAdmin.getEmail());
            log.info("Created tenant '{}' with admin user '{}'", tenant.getSlug(), admin.getEmail());
        };
    }

    private void validateBootstrapConfiguration() {
        if (!StringUtils.hasText(bootstrapProps.tenantName())) {
            throw new IllegalStateException("certify.bootstrap.admin.tenant-name must be configured when bootstrap is enabled");
        }
        if (!StringUtils.hasText(bootstrapProps.email())) {
            throw new IllegalStateException("certify.bootstrap.admin.email must be configured when bootstrap is enabled");
        }
        if (!StringUtils.hasText(bootstrapProps.password())) {
            throw new IllegalStateException("certify.bootstrap.admin.password must be configured when bootstrap is enabled");
        }
        if (!StringUtils.hasText(bootstrapProps.fullName())) {
            throw new IllegalStateException("certify.bootstrap.admin.full-name must be configured when bootstrap is enabled");
        }
        if (!StringUtils.hasText(bootstrapProps.systemEmail())) {
            throw new IllegalStateException("certify.bootstrap.admin.system-email must be configured when bootstrap is enabled");
        }
        if (!StringUtils.hasText(bootstrapProps.systemPassword())) {
            throw new IllegalStateException("certify.bootstrap.admin.system-password must be configured when bootstrap is enabled");
        }
        if (!StringUtils.hasText(bootstrapProps.systemFullName())) {
            throw new IllegalStateException("certify.bootstrap.admin.system-full-name must be configured when bootstrap is enabled");
        }
    }
}
