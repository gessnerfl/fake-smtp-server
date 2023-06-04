package de.gessnerfl.fakesmtp.model.query;

import jakarta.persistence.criteria.*;
import org.springframework.util.Assert;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public enum BinaryOperator {
    EQUAL {
        public <T> Predicate build(Path<T> path, CriteriaBuilder cb, Object value) {
            return cb.equal(path, value);
        }
    },
    NOT_EQUAL {
        public <T> Predicate build(Path<T> path, CriteriaBuilder cb, Object value) {
            return cb.notEqual(path, value);
        }
    },
    LIKE {
        public <T> Predicate build(Path<T> path, CriteriaBuilder cb, Object value) {
            Assert.isAssignable(path.getJavaType(), String.class, "Like operator is only supported for string values");
            return cb.like(cb.upper((Path<String>) path), "%" + value.toString().toUpperCase() + "%");
        }
    },
    LESS_THAN {
        public <T> Predicate build(Path<T> path, CriteriaBuilder cb, Object value) {
            return cb.lessThan((Path<? extends Comparable>)path, (Comparable) value);
        }
    },
    LESS_THAN_OR_EQUAL {
        public <T> Predicate build(Path<T> path, CriteriaBuilder cb, Object value) {
            return cb.lessThanOrEqualTo((Path<? extends Comparable>)path, (Comparable) value);
        }
    },
    GREATER_THAN {
        public <T> Predicate build(Path<T> path, CriteriaBuilder cb, Object value) {
            return cb.greaterThan((Path<? extends Comparable>)path, (Comparable) value);
        }
    },
    GREATER_THAN_OR_EQUAL {
        public <T> Predicate build(Path<T> path, CriteriaBuilder cb, Object value) {
            return cb.greaterThanOrEqualTo((Path<? extends Comparable>)path, (Comparable) value);
        }
    };    

    public abstract <T> Predicate build(Path<T> path, CriteriaBuilder cb, Object value);

}
