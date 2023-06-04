package de.gessnerfl.fakesmtp.model.query;

import jakarta.persistence.criteria.*;

public enum UnaryOperator {

    IS_NULL {
        public <T> Predicate build(Path<T> path, CriteriaBuilder cb) {
            return cb.isNull(path);
        }
    },

    NOT_NULL {
        public <T> Predicate build(Path<T> path, CriteriaBuilder cb) {
            return cb.isNotNull(path);
        }
    };

    public abstract <T> Predicate build(Path<T> path, CriteriaBuilder cb);

}
