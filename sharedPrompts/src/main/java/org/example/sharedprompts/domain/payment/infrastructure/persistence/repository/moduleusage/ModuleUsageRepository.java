package org.example.sharedprompts.domain.payment.infrastructure.persistence.repository.moduleusage;

import org.example.sharedprompts.domain.payment.domain.entity.ModuleUsage;
import org.example.sharedprompts.domain.payment.domain.enums.ModuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ModuleUsageRepository extends JpaRepository<ModuleUsage, Long> {

    @Query("SELECT COUNT(m) FROM ModuleUsage m WHERE m.user.id = :userId AND DATE(m.createdAt) = CURRENT_DATE")
    long countTodayByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM ModuleUsage m WHERE m.user.id = :userId AND m.moduleType = :moduleType AND DATE(m.createdAt) = CURRENT_DATE")
    long countTodayByUserIdAndModuleType(@Param("userId") Long userId, @Param("moduleType") ModuleType moduleType);
}
