package vn.hoadon.util;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import vn.hoadon.entity.TaxAuthorityEntity;

import java.util.ArrayList;
import java.util.List;

public class SearchCriteria {

    // Hàm tìm kiếm theo từ khóa (Keyword) - Giữ nguyên logic đã sửa trước đó
    public static Specification<TaxAuthorityEntity> hasKeyword(String keyword, String... fields) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) return cb.conjunction();
            
            String likePattern = "%" + keyword.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();

            for (String field : fields) {
                Path<?> path = root.get(field);
                if (path.getJavaType() == String.class) {
                    predicates.add(cb.like(cb.lower(path.as(String.class)), likePattern));
                } else {
                    predicates.add(cb.like(path.as(String.class), likePattern));
                }
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    // [MỚI] Hàm tìm kiếm chính xác (cho ParentId, Status)
    public static <T> Specification<T> hasEqual(String fieldName, Object value) {
        return (root, query, cb) -> {
            // Nếu value là null thì không lọc (trả về điều kiện luôn đúng)
            if (value == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get(fieldName), value);
        };
    }
}