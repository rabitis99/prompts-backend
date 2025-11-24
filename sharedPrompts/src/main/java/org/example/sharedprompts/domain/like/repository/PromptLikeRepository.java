package org.example.sharedprompts.domain.like.repository;

import org.example.sharedprompts.domain.like.PromptLike;
import org.example.sharedprompts.domain.like.PromptLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptLikeRepository extends JpaRepository<PromptLike, PromptLikeId> {
}
