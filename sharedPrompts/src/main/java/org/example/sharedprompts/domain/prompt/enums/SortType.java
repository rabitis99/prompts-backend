package org.example.sharedprompts.domain.prompt.enums;
import org.example.sharedprompts.domain.prompt.QPrompt;
import com.querydsl.core.types.OrderSpecifier;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SortType {

    LATEST("최신순", "최신순으로 정렬"){
        @Override
        public OrderSpecifier<?>[] toOrderSpecifiers(QPrompt prompt) {
            return new OrderSpecifier[]{prompt.createdAt.desc()};
        }
    },
    POPULAR("인기순", "인기순으로 정렬") {
        @Override
        public OrderSpecifier<?>[] toOrderSpecifiers(QPrompt prompt) {
            return new OrderSpecifier[]{prompt.viewCount.desc(), prompt.createdAt.desc()};
        }
    };

    private final String name;
    private final String description;

    public abstract OrderSpecifier<?>[] toOrderSpecifiers(QPrompt prompt);

}
