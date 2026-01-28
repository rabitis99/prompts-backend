package org.example.sharedprompts.domain.prompt.repository;

import org.example.sharedprompts.domain.prompt.Prompt;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PromptRepository extends JpaRepository<Prompt, Long>,CustomPromptRepository {
    @Query("""
        select p.id
        from Prompt p
        where p.id > :lastId
        order by p.id asc
    """)
    List<Long> findAllIds(@Param("lastId") Long lastId, Pageable pageable);

    default List<Long> findAllIds(Long lastId, int batchSize) {
        return findAllIds(
                lastId,
                PageRequest.of(0, batchSize)
        );
    }

    /**
     * 특정 기간 내 생성된 프롬프트 수 조회
     */
    @Query("SELECT COUNT(p) FROM Prompt p WHERE p.createdAt >= :startDate AND p.createdAt < :endDate")
    Long countCreatedPromptsBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 전체 조회 수 합계
     */
    @Query("SELECT SUM(p.viewCount) FROM Prompt p")
    Long sumTotalViewCount();

    /**
     * 전체 좋아요 수 합계
     */
    @Query("SELECT SUM(p.likeCount) FROM Prompt p")
    Long sumTotalLikeCount();

    /**
     * 특정 사용자가 작성한 프롬프트 수
     */
    @Query("SELECT COUNT(p) FROM Prompt p WHERE p.author.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자의 프롬프트들에 받은 총 좋아요 수
     */
    @Query("SELECT SUM(p.likeCount) FROM Prompt p WHERE p.author.id = :userId")
    Long sumLikeCountByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자의 통계 정보를 단일 쿼리로 조회
     * 성능 최적화를 위해 프롬프트 수와 총 좋아요 수를 한 번의 쿼리로 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 통계 Projection
     */
    @Query("""
        SELECT 
            COUNT(p) as promptCount,
            SUM(p.likeCount) as totalLikeCount
        FROM Prompt p 
        WHERE p.author.id = :userId
        """)
    UserStatisticsProjection getUserStatisticsByUserId(@Param("userId") Long userId);
}
