package vn.hoadon.ai.document.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.hoadon.ai.document.service.DocumentRecognitionService;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.entity.UserEntity;

@RestController
@RequestMapping("/v1/ai/document-recognition")
public class DocumentRecognitionController extends BaseController {
    private final DocumentRecognitionService recognitionService;

    public DocumentRecognitionController(DocumentRecognitionService recognitionService) {
        this.recognitionService = recognitionService;
    }

    @PostMapping(value = "/recognize", consumes = "multipart/form-data")
    public ResponseEntity<DocumentRecognitionDtos.RecognitionResponse> recognize(
            @RequestParam(defaultValue = "1") int category, @RequestPart("file") MultipartFile file) {
        permission("invoice-save");
        UserEntity user = currentUser();
        if (user == null || user.getId() == null || user.getCompanyId() == null) {
            throw new IllegalArgumentException("Không xác định được tài khoản hoặc doanh nghiệp");
        }
        return ResponseEntity.ok(recognitionService.recognize(user.getCompanyId(), category, file));
    }
}
