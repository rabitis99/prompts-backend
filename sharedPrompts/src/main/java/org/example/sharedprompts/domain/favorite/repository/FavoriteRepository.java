package org.example.sharedprompts.domain.favorite.repository;

import org.example.sharedprompts.domain.favorite.Favorite;
import org.example.sharedprompts.domain.favorite.FavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId>, CustomFavoriteRepository {
}

