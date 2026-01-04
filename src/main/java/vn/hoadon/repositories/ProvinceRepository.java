package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.hoadon.entity.ProvinceEntity;

import java.util.List;

@Repository
public interface ProvinceRepository extends JpaRepository<ProvinceEntity, Integer> {
    List<ProvinceEntity> findByStatus(Integer status);
}