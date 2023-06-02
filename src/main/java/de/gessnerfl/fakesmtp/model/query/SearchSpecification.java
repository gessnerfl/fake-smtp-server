package de.gessnerfl.fakesmtp.model.query;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@AllArgsConstructor
public class SearchSpecification<T> implements Specification<T> {

    private final transient SearchRequest request;

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Predicate predicate = cb.conjunction();
        List<Predicate> predicates = new ArrayList<>();

        for (FilterRequest filter : this.request.getFilters()) {
            Predicate filterPredicate = filter.getOperator().build(root, cb, filter, predicate);
            predicates.add(filterPredicate);
        }

        if (!predicates.isEmpty()) {
            if (request.getLogicalOperator() == LogicalOperator.AND) {
                predicate = cb.and(predicates.toArray(new Predicate[0]));
            } else {
                predicate = cb.or(predicates.toArray(new Predicate[0]));
            }
        }
        
        Predicate finalPredicate = null;
        if (!predicates.isEmpty()) {
            if (request.getLogicalOperator() == LogicalOperator.AND) {
                finalPredicate = cb.and(predicates.toArray(new Predicate[0]));
            } else {
                finalPredicate = cb.or(predicates.toArray(new Predicate[0]));
            }
        }

        List<Order> orders = new ArrayList<>();
        for (SortRequest sort : this.request.getSorts()) {
            orders.add(sort.getDirection().build(root, cb, sort));
        }

        query.orderBy(orders);
        return finalPredicate;
    }

    public static Pageable getPageable(Integer page, Integer size) {
        return PageRequest.of(Objects.requireNonNullElse(page, 0), Objects.requireNonNullElse(size, 100));
    }

}
