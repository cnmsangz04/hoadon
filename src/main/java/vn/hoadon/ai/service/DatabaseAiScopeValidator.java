package vn.hoadon.ai.service;

import org.springframework.stereotype.Component;
import vn.hoadon.ai.model.AiExecutionContext;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.UserRepository;

@Component
public class DatabaseAiScopeValidator implements AiScopeValidator {
    private final UserRepository userRepository;

    public DatabaseAiScopeValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void validate(AiExecutionContext context) {
        UserEntity user = userRepository.findById(context.userId())
                .orElseThrow(() -> new AiServiceException("AI_SCOPE_DENIED", "Tài khoản không có quyền dùng AI"));
        boolean active = Byte.valueOf((byte) 1).equals(user.getStatus()) && user.getDeletedAt() == null;
        if (!active || !context.companyId().equals(user.getCompanyId())) {
            throw new AiServiceException("AI_SCOPE_DENIED", "Tài khoản không có quyền dùng AI trong doanh nghiệp này");
        }
    }
}
