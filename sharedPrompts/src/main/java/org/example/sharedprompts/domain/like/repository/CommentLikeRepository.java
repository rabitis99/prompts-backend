package org.example.sharedprompts.domain.like.repository;

import org.example.sharedprompts.domain.like.CommentLike;
import org.example.sharedprompts.domain.like.CommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, CommentLikeId> {

}
