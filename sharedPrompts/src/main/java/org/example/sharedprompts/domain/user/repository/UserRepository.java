package org.example.sharedprompts.domain.user.repository;

import org.example.sharedprompts.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
}
