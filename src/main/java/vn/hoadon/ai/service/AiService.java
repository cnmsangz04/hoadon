package vn.hoadon.ai.service;

import vn.hoadon.ai.model.AiExecutionContext;
import vn.hoadon.ai.model.AiRequest;
import vn.hoadon.ai.model.AiResponse;

public interface AiService {
    AiResponse generate(AiExecutionContext context, AiRequest request);
}
