package vn.hoadon.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

import vn.hoadon.entity.FormInvoiceEntity;

@Repository
public class FormInvoiceTemplateRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Page<FormInvoiceEntity> searchTemplates(String q, Integer category, Integer type, Integer system, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Tạo query chính
        CriteriaQuery<FormInvoiceEntity> cq = cb.createQuery(FormInvoiceEntity.class);
        Root<FormInvoiceEntity> root = cq.from(FormInvoiceEntity.class);
        List<Predicate> predicates = new ArrayList<>();

        if (q != null && !q.trim().isEmpty()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(root.get("name")), like));
        }
        if (category != null) {
            predicates.add(cb.equal(root.get("category"), category));
        }
        if (type != null) {
            predicates.add(cb.equal(root.get("type"), type));
        }
        if (system != null) {
            predicates.add(cb.equal(root.get("system"), system));
        }

        if (!predicates.isEmpty()) {
            cq.where(cb.and(predicates.toArray(new Predicate[0])));
        }
        cq.orderBy(cb.desc(root.get("updatedAt")));

        TypedQuery<FormInvoiceEntity> query = entityManager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<FormInvoiceEntity> content = query.getResultList();

        // Tạo query đếm
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<FormInvoiceEntity> countRoot = countQuery.from(FormInvoiceEntity.class);
        List<Predicate> countPredicates = new ArrayList<>();
        if (q != null && !q.trim().isEmpty()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            countPredicates.add(cb.like(cb.lower(countRoot.get("name")), like));
        }
        if (category != null) {
            countPredicates.add(cb.equal(countRoot.get("category"), category));
        }
        if (type != null) {
            countPredicates.add(cb.equal(countRoot.get("type"), type));
        }
        if (system != null) {
            countPredicates.add(cb.equal(countRoot.get("system"), system));
        }
        if (!countPredicates.isEmpty()) {
            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }
        countQuery.select(cb.count(countRoot));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }
}
