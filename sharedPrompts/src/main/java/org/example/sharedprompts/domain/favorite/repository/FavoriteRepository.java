package org.example.sharedprompts.domain.favorite.repository;

import org.example.sharedprompts.domain.favorite.Favorite;
import org.example.sharedprompts.domain.favorite.FavoriteId;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {

    @Query("SELECT f.prompt FROM Favorite f WHERE f.user.id = :userId")
    Page<Prompt> findPromptsByUserId(@Param("userId") Long userId, Pageable pageable);
}

