package vn.hoadon.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import vn.hoadon.repositories.UserRepository;
import vn.hoadon.services.UserService;
import vn.hoadon.entity.UserEntity;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
