package org.example.sharedprompts.domain.tag.repository;

import org.example.sharedprompts.domain.tag.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {}
