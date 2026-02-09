package org.example.sharedprompts.infra.production.blog;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 블로그 콘텐츠를 구성하는 컴포저.
 * Production 계층에서 사용되며, 외부 시스템 통신 없이 순수하게 콘텐츠만 생성한다.
 */
@Component
public class BlogComposer {
    
    /**
     * 블로그 콘텐츠를 구성한다.
     * 
     * @param title 블로그 제목
     * @param content 프롬프트로부터 생성된 본문 내용
     * @param tags 태그 리스트
     * @return 구성된 블로그 콘텐츠 (마크다운 형식)
     */
    public String compose(String title, String content, List<String> tags) {
        StringBuilder blogContent = new StringBuilder();
        
        // 제목
        blogContent.append("# ").append(title).append("\n\n");
        
        // 본문
        blogContent.append(content).append("\n\n");
        
        // 태그
        if (tags != null && !tags.isEmpty()) {
            blogContent.append("---\n\n");
            blogContent.append("**Tags:** ");
            String tagString = tags.stream()
                    .map(tag -> "#" + tag)
                    .collect(Collectors.joining(" "));
            blogContent.append(tagString).append("\n");
        }
        
        return blogContent.toString();
    }
}

