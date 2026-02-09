package org.example.sharedprompts.infra.production.email;

import org.springframework.stereotype.Component;

/**
 * 이메일 콘텐츠를 구성하는 컴포저.
 * Production 계층에서 사용되며, 외부 시스템 통신 없이 순수하게 콘텐츠만 생성한다.
 */
@Component
public class EmailComposer {
    
    /**
     * 이메일 콘텐츠를 구성한다.
     * 
     * @param subject 이메일 제목
     * @param content 프롬프트로부터 생성된 본문 내용
     * @param recipient 수신자 이메일 주소
     * @return 구성된 이메일 콘텐츠 (HTML 형식)
     */
    public String compose(String subject, String content, String recipient) {
        StringBuilder emailContent = new StringBuilder();
        
        // HTML 이메일 형식으로 구성
        emailContent.append("<!DOCTYPE html>\n");
        emailContent.append("<html>\n");
        emailContent.append("<head>\n");
        emailContent.append("  <meta charset=\"UTF-8\">\n");
        emailContent.append("  <title>").append(escapeHtml(subject)).append("</title>\n");
        emailContent.append("</head>\n");
        emailContent.append("<body>\n");
        emailContent.append("  <div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">\n");
        emailContent.append("    <h2>").append(escapeHtml(subject)).append("</h2>\n");
        emailContent.append("    <div style=\"line-height: 1.6; color: #333;\">\n");
        
        // 본문 내용을 HTML로 변환 (간단한 줄바꿈 처리)
        String htmlContent = content.replace("\n", "<br>\n");
        emailContent.append("      ").append(htmlContent).append("\n");
        
        emailContent.append("    </div>\n");
        emailContent.append("  </div>\n");
        emailContent.append("</body>\n");
        emailContent.append("</html>\n");
        
        return emailContent.toString();
    }
    
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}

