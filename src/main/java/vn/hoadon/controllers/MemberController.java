package vn.hoadon.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.services.MemberService;

@RestController
@RequestMapping("/v1/administrator/members")
public class MemberController {

    @Autowired
    private MemberService service;

    @PostMapping("/list")
    public Page<UserEntity> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Byte status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return service.list(keyword, roleId, status, pageable);
    }

    @PostMapping("/saveOrUpdate")
    public UserEntity saveOrUpdate(@RequestBody UserEntity incoming) {
        return service.saveOrUpdate(incoming);
    }

    @PostMapping("/{id}/lock")
    public void lock(@PathVariable Long id, @RequestParam("lock") int lock) {
        service.setLock(id, lock == 1);
    }

    @PostMapping("/{id}/reset-password")
    public void resetPassword(@PathVariable Long id) {
        service.resetPassword(id);
    }

    @DeleteMapping("/{id}")
    public void removeFromCompany(@PathVariable Long id) {
        service.removeFromCompany(id);
    }
}
