package vn.hoadon.utils;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class SearchCriteria {
    // Giúp tạo query: WHERE (name LIKE %key% OR code LIKE %key%) AND status = active
    public static <T> Specification<T> hasKeyword(String keyword, String... fields) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) return cb.conjunction();
            String likePattern = "%" + keyword.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();
            for (String field : fields) {
                predicates.add(cb.like(cb.lower(root.get(field)), likePattern));
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}