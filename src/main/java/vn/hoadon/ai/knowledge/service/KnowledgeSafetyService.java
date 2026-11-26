package vn.hoadon.ai.knowledge.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class KnowledgeSafetyService {
    private static final Pattern INJECTION = Pattern.compile(
            "(?iu)(ignore|bỏ qua|quên).{0,30}(previous|trước|system|instruction|chỉ dẫn)"
                    + "|(?iu)(system prompt|developer message|tiết lộ.{0,20}(prompt|mật khẩu|token))"
                    + "|(?iu)(act as|đóng vai).{0,40}(system|admin|developer)"
                    + "|(?iu)(execute|thực thi|run).{0,20}(code|command|lệnh|script)");

    public boolean hasPromptInjection(String content) {
        return content != null && INJECTION.matcher(content).find();
    }

    public String prepareForExternal(String content) {
        if (content == null || content.isBlank()) return "";
        StringBuilder safe = new StringBuilder();
        for (String line : content.split("\\R", -1)) {
            if (INJECTION.matcher(line).find()) {
                safe.append("[Dòng có chỉ dẫn không đáng tin cậy đã bị loại bỏ]");
            } else {
                safe.append(line);
            }
            safe.append('\n');
        }
        return safe.toString().trim();
    }
}
