package org.example.sharedprompts.domain.tag.repository;

import org.example.sharedprompts.domain.tag.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);

    @Modifying(clearAutomatically = true)
    @Query("update Tag t set t.count = t.count + 1 where t.name = :name")
    void incrementCount(@Param("name") String name);

    @Modifying(clearAutomatically = true)
    @Query("update Tag t set t.count = t.count - 1 where t.name = :name")
    void decrementCount(@Param("name") String name);
}
