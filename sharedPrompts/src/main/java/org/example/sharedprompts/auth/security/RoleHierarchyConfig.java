package org.example.sharedprompts.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;

/**
 * Spring Security Role 계층 구조 설정
 *
 * <p>Role 계층 구조를 정의하여 상위 Role이 하위 Role의 권한을 자동으로 상속받도록 합니다.
 *
 * <p>현재 계층 구조:
 * <ul>
 *   <li>ROLE_ADMIN > ROLE_USER</li>
 * </ul>
 *
 * <p>이 설정을 통해:
 * <ul>
 *   <li>ROLE_ADMIN은 ROLE_USER의 모든 권한을 자동으로 가집니다.</li>
 *   <li>@PreAuthorize("hasRole('USER')")로 설정된 엔드포인트도 ROLE_ADMIN이 접근 가능합니다.</li>
 *   <li>@PreAuthorize("hasAnyRole('ADMIN', 'USER')") 대신 @PreAuthorize("hasRole('USER')")만으로 충분합니다.</li>
 * </ul>
 *
 * <p>사용 예시:
 * <pre>{@code
 * // ROLE_USER만 허용하지만, ROLE_ADMIN도 접근 가능 (계층 구조로 인해)
 * @PreAuthorize("hasRole('USER')")
 * @GetMapping("/prompts")
 * public ResponseEntity<...> getPrompts(...) {
 *     // ...
 * }
 *
 * // ROLE_ADMIN만 허용
 * @PreAuthorize("hasRole('ADMIN')")
 * @GetMapping("/admin/users")
 * public ResponseEntity<...> getUsers(...) {
 *     // ...
 * }
 * }</pre>
 */
@Configuration
public class RoleHierarchyConfig {

    /**
     * Role 계층 구조 Bean 생성
     *
     * <p>계층 구조 표현식:
     * <ul>
     *   <li>"ROLE_ADMIN > ROLE_USER": ROLE_ADMIN이 ROLE_USER보다 상위</li>
     *   <li>공백으로 구분된 여러 계층 정의 가능</li>
     * </ul>
     *
     * <p>참고: RoleHierarchyImpl과 setHierarchy()는 deprecated되었지만,
     * Spring Security에서 아직 공식 대체 API가 제공되지 않아 현재 방식으로 사용합니다.
     * Bean으로 등록되면 자동으로 Spring Security에 적용됩니다.
     *
     * @return Role 계층 구조
     */
    @Bean
    @SuppressWarnings("deprecation")
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy("ROLE_ADMIN > ROLE_USER");
        return hierarchy;
    }
}

