package org.example.sharedprompts.domain.tag;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.global.entity.BaseEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "tags",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tag_name",
                        columnNames = "name"
                )
        },
        indexes = {
                // 추가된 인덱스 목록 (우선순위: 선택)
                // 인기 태그 조회 최적화
                @Index(name = "idx_tags_count", columnList = "count DESC")
        }
)
public class Tag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Long count;

    @Builder(toBuilder = true)
    public Tag(String name) {
        this.name = name;
        this.count = 0L;
    }
}
