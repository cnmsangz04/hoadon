package vn.hoadon.services.impl;

import org.springframework.stereotype.Service;
import vn.hoadon.entity.BankEntity;
import vn.hoadon.repositories.BankRepository;
import vn.hoadon.services.BankService;

import java.util.List;
import java.util.Optional;

@Service
public class BankServiceImpl implements BankService {

    private final BankRepository bankRepository;

    public BankServiceImpl(BankRepository bankRepository) {
        this.bankRepository = bankRepository;
    }

    @Override
    public List<BankEntity> list(Integer status) {
        if (status == null) return bankRepository.findAll();
        return bankRepository.findByStatus(status);
    }

    @Override
    public Optional<BankEntity> findByAbbreviation(String abbreviation) {
        if (abbreviation == null || abbreviation.isBlank()) return Optional.empty();
        List<BankEntity> list = bankRepository.findByAbbreviation(abbreviation);
        if (list == null || list.isEmpty()) return Optional.empty();
        return Optional.of(list.get(0));
    }

    @Override
    public Optional<BankEntity> findByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        List<BankEntity> list = bankRepository.findByName(name);
        if (list == null || list.isEmpty()) return Optional.empty();
        return Optional.of(list.get(0));
    }
}