package vn.hoadon.ai.model;

public record AiExecutionContext(Long userId, Long companyId, AiFeature feature) {
    public AiExecutionContext {
        if (userId == null || companyId == null || feature == null) {
            throw new IllegalArgumentException("Thiếu phạm vi tài khoản hoặc doanh nghiệp cho yêu cầu AI");
        }
    }
}
