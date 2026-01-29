package org.example.sharedprompts.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.UserTerms;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 어드민 계정 초기화
 * 
 * <p>애플리케이션 시작 시 환경 변수로 설정된 어드민 계정 정보를 사용하여
 * 어드민 계정이 없으면 생성합니다.
 * 
 * <p>보안을 위해 비밀번호는 환경 변수(ADMIN_PASSWORD)로 받아서 BCrypt로 해싱하여 저장합니다.
 * 
 * <p>환경 변수:
 * <ul>
 *   <li>ADMIN_EMAIL: 어드민 이메일 (기본값: admin@sharedprompts.com)</li>
 *   <li>ADMIN_PASSWORD: 어드민 비밀번호 (필수, 프로덕션 환경)</li>
 *   <li>ADMIN_NICKNAME: 어드민 닉네임 (기본값: 관리자)</li>
 * </ul>
 * 
 * <p>주의사항:
 * <ul>
 *   <li>프로덕션 환경에서는 반드시 ADMIN_PASSWORD를 설정해야 합니다.</li>
 *   <li>비밀번호는 BCrypt로 해싱되어 저장되므로 원문은 복구할 수 없습니다.</li>
 *   <li>이미 존재하는 어드민 계정은 업데이트하지 않습니다.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1) // Flyway 마이그레이션 이후 실행되도록 설정
public class AdminAccountInitializer implements CommandLineRunner {

    private static final String DEFAULT_ADMIN_EMAIL = "admin@sharedprompts.com";
    private static final String DEFAULT_ADMIN_NICKNAME = "관리자";
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    @Transactional
    public void run(String... args) {
        String adminEmail = environment.getProperty("ADMIN_EMAIL", DEFAULT_ADMIN_EMAIL);
        String adminPassword = environment.getProperty("ADMIN_PASSWORD");
        String adminNickname = environment.getProperty("ADMIN_NICKNAME", DEFAULT_ADMIN_NICKNAME);
        
        // 프로덕션 환경에서 비밀번호 필수 검증
        boolean isProdProfile = containsProfile(environment.getActiveProfiles(), "prod");
        if (isProdProfile && (adminPassword == null || adminPassword.trim().isEmpty())) {
            log.warn("⚠️ 프로덕션 환경에서 ADMIN_PASSWORD가 설정되지 않았습니다. " +
                    "환경 변수 ADMIN_PASSWORD를 설정하거나, 기존 어드민 계정이 있는지 확인하세요.");
            // 프로덕션 환경에서도 기존 계정이 있으면 계속 진행
            checkExistingAdmin(adminEmail);
            return;
        }
        
        // 개발 환경에서 비밀번호가 없으면 기본 비밀번호 사용 (경고)
        if (!isProdProfile && (adminPassword == null || adminPassword.trim().isEmpty())) {
            log.warn("⚠️ 개발 환경에서 ADMIN_PASSWORD가 설정되지 않았습니다. " +
                    "기본 비밀번호를 사용합니다. 프로덕션 환경에서는 반드시 환경 변수로 설정하세요.");
            adminPassword = "Admin123!@#"; // 개발 환경 기본 비밀번호
        }
        
        // 어드민 계정 생성 또는 확인
        createAdminIfNotExists(adminEmail, adminPassword, adminNickname);
    }

    /**
     * 어드민 계정이 없으면 생성, 있으면 Role 확인 및 업데이트
     */
    private void createAdminIfNotExists(String email, String password, String nickname) {
        Optional<User> existingAdmin = userRepository.findByProviderAndProviderId(
                Provider.LOCAL, email);
        
        if (existingAdmin.isPresent()) {
            User admin = existingAdmin.get();
            if (admin.isDeleted()) {
                log.info("✅ 삭제된 어드민 계정이 발견되었습니다. 복구하지 않습니다. (이메일: {})", email);
                return;
            }
            
            // 기존 계정의 Role이 ROLE_ADMIN이 아니면 업데이트
            if (admin.getRole() != Role.ROLE_ADMIN) {
                log.warn("⚠️ 기존 계정의 Role이 ROLE_USER입니다. ROLE_ADMIN으로 업데이트합니다. (이메일: {}, ID: {}, 현재 Role: {})", 
                        email, admin.getId(), admin.getRole());
                admin.changeRole(Role.ROLE_ADMIN);
                userRepository.save(admin);
                log.info("✅ 어드민 계정의 Role이 업데이트되었습니다. (이메일: {}, ID: {})", email, admin.getId());
            } else {
                log.info("✅ 어드민 계정이 이미 존재합니다. (이메일: {}, ID: {})", email, admin.getId());
            }
            return;
        }
        
        // 어드민 계정 생성
        String encodedPassword = passwordEncoder.encode(password);
        User admin = User.builder()
                .email(email)
                .provider(Provider.LOCAL)
                .providerId(email)
                .password(encodedPassword)
                .nickname(nickname)
                .role(Role.ROLE_ADMIN)
                .terms(UserTerms.builder()
                        .required(true)
                        .privacy(true)
                        .marketing(false)
                        .build())
                .signupCompleted(true)
                .blocked(false)
                .build();
        
        userRepository.save(admin);
        log.info("✅ 어드민 계정이 생성되었습니다. (이메일: {}, 닉네임: {})", email, nickname);
    }

    /**
     * 기존 어드민 계정 확인 및 Role 업데이트 (프로덕션 환경에서 비밀번호가 없을 때)
     */
    private void checkExistingAdmin(String email) {
        Optional<User> existingAdmin = userRepository.findByProviderAndProviderId(
                Provider.LOCAL, email);
        
        if (existingAdmin.isPresent() && !existingAdmin.get().isDeleted()) {
            User admin = existingAdmin.get();
            // Role이 ROLE_ADMIN이 아니면 업데이트
            if (admin.getRole() != Role.ROLE_ADMIN) {
                log.warn("⚠️ 기존 계정의 Role이 ROLE_USER입니다. ROLE_ADMIN으로 업데이트합니다. (이메일: {}, ID: {}, 현재 Role: {})", 
                        email, admin.getId(), admin.getRole());
                admin.changeRole(Role.ROLE_ADMIN);
                userRepository.save(admin);
                log.info("✅ 어드민 계정의 Role이 업데이트되었습니다. (이메일: {}, ID: {})", email, admin.getId());
            } else {
                log.info("✅ 기존 어드민 계정이 존재합니다. (이메일: {})", email);
            }
        } else {
            log.error("❌ 프로덕션 환경에서 어드민 계정이 없고 ADMIN_PASSWORD도 설정되지 않았습니다. " +
                    "환경 변수 ADMIN_PASSWORD를 설정하고 애플리케이션을 재시작하세요.");
        }
    }

    private boolean containsProfile(String[] profiles, String profile) {
        for (String p : profiles) {
            if (p.equals(profile)) {
                return true;
            }
        }
        return false;
    }
}

