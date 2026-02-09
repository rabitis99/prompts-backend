package org.example.sharedprompts.module.dto.request.production;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailCommandDto implements CommandDto {
    private String subject;
    private String recipient;
}

