package vn.hoadon.services;

import vn.hoadon.dto.bank.BankFilterDTO;
import vn.hoadon.entity.BankEntity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.*;

public interface BankService {
	Page<BankEntity> list(BankFilterDTO filter, Pageable pageable);
    List<BankEntity> list(Integer status);
    BankEntity saveOrUpdate(BankEntity bank);
    void setLock(Long id, boolean lock);
    Optional<BankEntity> findByAbbreviation(String abbreviation);
    Optional<BankEntity> findByName(String name);
}