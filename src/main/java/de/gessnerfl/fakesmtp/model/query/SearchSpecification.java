package de.gessnerfl.fakesmtp.model.query;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;

public class SearchSpecification<T> implements Specification<T> {

    private final transient SearchRequest request;

    public SearchSpecification(SearchRequest request) {
        this.request = request;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return request.getFilter().map(e -> e.toPredicate(root, query, cb)).orElse(null);
    }

}
