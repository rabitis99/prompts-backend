package org.example.sharedprompts.controller.prompt;

import org.example.sharedprompts.domain.prompt.enums.StyleType;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.ToneType;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.prompt.response.DomainMetadataResponseDto;
import org.example.sharedprompts.dto.prompt.response.SimpleDomainResponseDto;
import org.example.sharedprompts.dto.prompt.response.SimpleStyleResponseDto;
import org.example.sharedprompts.dto.prompt.response.SimpleToneResponseDto;
import org.example.sharedprompts.dto.prompt.response.StyleMetadataResponseDto;
import org.example.sharedprompts.dto.prompt.response.ToneMetadataResponseDto;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

import java.util.Arrays;
import java.util.List;

/**
 * 프롬프트 메타데이터 API 컨트롤러
 * <p>TaskDomain, StyleType, ToneType 간의 연결성 정보를 제공한다.</p>
 */
@RestController
@RequestMapping("/prompts/metadata")
public class PromptMetadataController {

    /**
     * 모든 TaskDomain의 메타데이터 조회
     * <p>각 도메인별 추천 StyleType과 ToneType 목록을 포함한다.</p>
     */
    @GetMapping("/domains")
    public ResponseEntity<CustomResponse<List<DomainMetadataResponseDto>>> getAllDomainMetadata() {
        List<DomainMetadataResponseDto> metadata = Arrays.stream(TaskDomain.values())
                .map(DomainMetadataResponseDto::from)
                .toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .body(CustomResponse.ok(metadata));
    }

    /**
     * 특정 TaskDomain의 메타데이터 조회
     */
    @GetMapping("/domains/{domain}")
    public ResponseEntity<CustomResponse<DomainMetadataResponseDto>> getDomainMetadata(
            @PathVariable TaskDomain domain
    ) {
        DomainMetadataResponseDto metadata = DomainMetadataResponseDto.from(domain);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .body(CustomResponse.ok(metadata));
    }

    /**
     * 특정 TaskDomain에 추천되는 StyleType 목록 조회
     */
    @GetMapping("/domains/{domain}/styles")
    public ResponseEntity<CustomResponse<List<SimpleStyleResponseDto>>> getRecommendedStyles(
            @PathVariable TaskDomain domain
    ) {
        List<SimpleStyleResponseDto> styles = domain.getRecommendedStyles().stream()
                .map(SimpleStyleResponseDto::from)
                .toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .body(CustomResponse.ok(styles));
    }

    /**
     * 특정 TaskDomain에 추천되는 ToneType 목록 조회
     */
    @GetMapping("/domains/{domain}/tones")
    public ResponseEntity<CustomResponse<List<SimpleToneResponseDto>>> getRecommendedTones(
            @PathVariable TaskDomain domain
    ) {
        List<SimpleToneResponseDto> tones = domain.getRecommendedTones().stream()
                .map(SimpleToneResponseDto::from)
                .toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .body(CustomResponse.ok(tones));
    }

    /**
     * 모든 StyleType의 메타데이터 조회
     * <p>각 스타일별 추천 TaskDomain 목록을 포함한다.</p>
     */
    @GetMapping("/styles")
    public ResponseEntity<CustomResponse<List<StyleMetadataResponseDto>>> getAllStyleMetadata() {
        List<StyleMetadataResponseDto> metadata = Arrays.stream(StyleType.values())
                .map(StyleMetadataResponseDto::from)
                .toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .body(CustomResponse.ok(metadata));
    }

    /**
     * 특정 StyleType의 메타데이터 조회
     */
    @GetMapping("/styles/{style}")
    public ResponseEntity<CustomResponse<StyleMetadataResponseDto>> getStyleMetadata(
            @PathVariable StyleType style
    ) {
        StyleMetadataResponseDto metadata = StyleMetadataResponseDto.from(style);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .body(CustomResponse.ok(metadata));
    }

    /**
     * 특정 StyleType에 추천되는 TaskDomain 목록 조회
     */
    @GetMapping("/styles/{style}/domains")
    public ResponseEntity<CustomResponse<List<SimpleDomainResponseDto>>> getRecommendedDomainsForStyle(
            @PathVariable StyleType style
    ) {
        List<SimpleDomainResponseDto> domains = style.getRecommendedDomains().stream()
                .map(SimpleDomainResponseDto::from)
                .toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .body(CustomResponse.ok(domains));
    }

    /**
     * 모든 ToneType의 메타데이터 조회
     * <p>각 톤별 추천 TaskDomain 목록을 포함한다.</p>
     */
    @GetMapping("/tones")
    public ResponseEntity<CustomResponse<List<ToneMetadataResponseDto>>> getAllToneMetadata() {
        List<ToneMetadataResponseDto> metadata = Arrays.stream(ToneType.values())
                .map(ToneMetadataResponseDto::from)
                .toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .body(CustomResponse.ok(metadata));
    }

    /**
     * 특정 ToneType의 메타데이터 조회
     */
    @GetMapping("/tones/{tone}")
    public ResponseEntity<CustomResponse<ToneMetadataResponseDto>> getToneMetadata(
            @PathVariable ToneType tone
    ) {
        ToneMetadataResponseDto metadata = ToneMetadataResponseDto.from(tone);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .body(CustomResponse.ok(metadata));
    }

    /**
     * 특정 ToneType에 추천되는 TaskDomain 목록 조회
     */
    @GetMapping("/tones/{tone}/domains")
    public ResponseEntity<CustomResponse<List<SimpleDomainResponseDto>>> getRecommendedDomainsForTone(
            @PathVariable ToneType tone
    ) {
        List<SimpleDomainResponseDto> domains = tone.getRecommendedDomains().stream()
                .map(SimpleDomainResponseDto::from)
                .toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .body(CustomResponse.ok(domains));
    }
}

