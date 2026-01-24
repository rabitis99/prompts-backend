package org.example.sharedprompts.domain.favorite.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.favorite.Favorite;
import org.example.sharedprompts.domain.favorite.FavoriteId;
import org.example.sharedprompts.domain.favorite.event.FavoriteEvent;
import org.example.sharedprompts.domain.favorite.repository.FavoriteRepository;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.repository.PromptRepository;
import org.example.sharedprompts.domain.tag.PromptTag;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.favorite.response.FavoriteResponseDto;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.dto.common.PageResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final PromptRepository promptRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void addFavorite(Long userId, Long promptId) {
        validateUserExists(userId);
        validatePromptExists(promptId);

        FavoriteId id = createFavoriteId(userId, promptId);
        Favorite favorite = createFavorite(userId, promptId, id);
        
        try {
            favoriteRepository.save(favorite);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.PROMPT_ALREADY_FAVORITED);
        }

        eventPublisher.publishEvent(new FavoriteEvent.PromptFavorited(userId, promptId));
    }

    @Override
    @Transactional
    public void removeFavorite(Long userId, Long promptId) {
        FavoriteId id = createFavoriteId(userId, promptId);
        validateFavoriteExists(id);

        favoriteRepository.deleteById(id);

        eventPublisher.publishEvent(new FavoriteEvent.PromptUnfavorited(promptId));
    }

    @Override
    @Transactional(readOnly = true)
    public FavoriteResponseDto checkFavorite(Long userId, Long promptId) {
        FavoriteId id = createFavoriteId(userId, promptId);
        boolean isFavorite = favoriteRepository.existsById(id);
        return FavoriteResponseDto.from(isFavorite);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromptResponseDto> getFavoritePrompts(Long userId, Pageable pageable) {
        Page<Prompt> page = favoriteRepository.findPromptsByUserId(userId, pageable);
        return PageResponse.of(page.map(this::toPromptResponseDto));
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validatePromptExists(Long promptId) {
        if (!promptRepository.existsById(promptId)) {
            throw new ApiException(ErrorCode.PROMPT_NOT_FOUND);
        }
    }

    private FavoriteId createFavoriteId(Long userId, Long promptId) {
        return new FavoriteId(userId, promptId);
    }

    private void validateFavoriteExists(FavoriteId id) {
        if (!favoriteRepository.existsById(id)) {
            throw new ApiException(ErrorCode.PROMPT_FAVORITE_NOT_FOUND);
        }
    }

    private Favorite createFavorite(Long userId, Long promptId, FavoriteId id) {
        User user = userRepository.getReferenceById(userId);
        Prompt prompt = promptRepository.getReferenceById(promptId);

        return Favorite.builder()
                .id(id)
                .user(user)
                .prompt(prompt)
                .build();
    }

    private PromptResponseDto toPromptResponseDto(Prompt prompt) {
        return PromptResponseDto.from(
                prompt,
                prompt.getPromptTags().stream()
                        .map(PromptTag::getTag)
                        .toList()
        );
    }
}

