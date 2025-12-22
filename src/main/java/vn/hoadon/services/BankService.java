package vn.hoadon.services;

import vn.hoadon.entity.BankEntity;

import java.util.List;
import java.util.Optional;

public interface BankService {
    List<BankEntity> list(Integer status);
    Optional<BankEntity> findByAbbreviation(String abbreviation);
    Optional<BankEntity> findByName(String name);
}