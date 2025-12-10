package org.example.sharedprompts.domain.tag;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.global.entity.BaseEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "tags")
public class Tag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String name;

    private Long count;

    @Builder(toBuilder = true)
    public Tag(String name) {
        this.name = name;
    }

    public void increaseCount() { this.count = (this.count == null ? 1L : this.count + 1); }
    public void decreaseCount() { this.count = (this.count == null || this.count <= 0 ? 0L : this.count - 1); }
}
