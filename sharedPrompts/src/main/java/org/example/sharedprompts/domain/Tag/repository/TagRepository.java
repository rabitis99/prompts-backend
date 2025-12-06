package org.example.sharedprompts.domain.Tag.repository;

import org.example.sharedprompts.domain.Tag.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag>  findByName(String name);
}
