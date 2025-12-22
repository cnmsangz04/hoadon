package vn.hoadon.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.hoadon.dto.UserDto;
import vn.hoadon.services.UserService;

@RestController
@Validated
@RequestMapping(
    value = "v1/setting/account",
    produces = MediaType.APPLICATION_JSON_VALUE
)
public class AccountController {

    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/info")
    public ResponseEntity<UserDto> getInfo() {
        return ResponseEntity.ok(userService.getCurrentUserInfo());
    }

    @PostMapping(
        value = "/update",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserDto> update(
            @RequestBody UserDto update
    ) {
        return ResponseEntity.ok(
            userService.updateCurrentUser(update)
        );
    }

    @PostMapping(
        value = "/avatar",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UserDto> updateAvatar(
            @RequestParam("avatar") MultipartFile avatar
    ) {
        return ResponseEntity.ok(
            userService.updateAvatar(avatar)
        );
    }
}
