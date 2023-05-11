package de.gessnerfl.fakesmtp.model.query;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public enum Operator {

    EQUAL {
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, FilterRequest request, Predicate predicate) {
            if (request.getValue() == null) {
                return predicate;
            }

            Object value = request.getFieldType().parse(request.getValue().toString());
            Expression<?> key = root.get(request.getKey());
            return cb.and(cb.equal(key, value), predicate);
        }
    },

    NOT_EQUAL {
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, FilterRequest request, Predicate predicate) {
            Object value = request.getFieldType().parse(request.getValue().toString());
            Expression<?> key = root.get(request.getKey());
            return cb.and(cb.notEqual(key, value), predicate);
        }
    },

    LIKE {
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, FilterRequest request, Predicate predicate) {
            Expression<String> key = root.get(request.getKey());
            return cb.and(cb.like(cb.upper(key), "%" + request.getValue().toString().toUpperCase() + "%"), predicate);
        }
    },

    IN {
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, FilterRequest request, Predicate predicate) {
            List<Object> values = request.getValues();
            CriteriaBuilder.In<Object> inClause = cb.in(root.get(request.getKey()));
            for (Object value : values) {
                inClause.value(request.getFieldType().parse(value.toString()));
            }
            return cb.and(inClause, predicate);
        }
    },

    BETWEEN {
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, FilterRequest request, Predicate predicate) {
            Object value = request.getFieldType().parse(request.getValue().toString());
            Object valueTo = request.getFieldType().parse(request.getValueTo().toString());
            if (request.getFieldType() == FieldType.DATE) {
                LocalDateTime startDate = (LocalDateTime) value;
                LocalDateTime endDate = (LocalDateTime) valueTo;

                Timestamp startTimestamp = Timestamp.valueOf(startDate);
                Timestamp endTimestamp = Timestamp.valueOf(endDate);

                Expression<Timestamp> key = root.get(request.getKey());
                return cb.and(cb.and(cb.greaterThanOrEqualTo(key, startTimestamp), cb.lessThanOrEqualTo(key, endTimestamp)), predicate);
            }

            if (request.getFieldType() != FieldType.CHAR && request.getFieldType() != FieldType.BOOLEAN) {
                Number start = (Number) value;
                Number end = (Number) valueTo;
                Expression<Number> key = root.get(request.getKey());
                return cb.and(cb.and(cb.ge(key, start), cb.le(key, end)), predicate);
            }

            return predicate;
        }
    }, 

    IS_NULL {
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, FilterRequest request, Predicate predicate) {
            Expression<?> key = root.get(request.getKey());
            return cb.and(cb.isNull(key), predicate);
        }
    },

    NOT_NULL {
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, FilterRequest request, Predicate predicate) {
            Expression<?> key = root.get(request.getKey());
            return cb.and(cb.isNotNull(key), predicate);
        }
    },

    LESS_THAN {
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, FilterRequest request, Predicate predicate) {
            Object value = request.getFieldType().parse(request.getValue().toString());
            Expression<? extends Comparable> key = root.get(request.getKey());
    
            if (request.getFieldType() == FieldType.DATE) {
                LocalDateTime dateValue = (LocalDateTime) value;
                Expression<LocalDateTime> keyAsLocalDateTime = key.as(LocalDateTime.class);
                return cb.and(cb.lessThan(keyAsLocalDateTime, dateValue), predicate);
            }
    
            return cb.and(cb.lessThan(key, (Comparable) value), predicate);
        }
    },

    LESS_THAN_OR_EQUAL {
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, FilterRequest request, Predicate predicate) {
            Object value = request.getFieldType().parse(request.getValue().toString());
            Expression<? extends Comparable> key = root.get(request.getKey());
    
            if (request.getFieldType() == FieldType.DATE) {
                LocalDateTime dateValue = (LocalDateTime) value;
                Expression<LocalDateTime> keyAsLocalDateTime = key.as(LocalDateTime.class);
                return cb.and(cb.lessThanOrEqualTo(keyAsLocalDateTime, dateValue), predicate);
            }
    
            return cb.and(cb.lessThanOrEqualTo(key, (Comparable) value), predicate);
        }
    },

    GREATER_THAN {
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, FilterRequest request, Predicate predicate) {
            Object value = request.getFieldType().parse(request.getValue().toString());
            Expression<? extends Comparable> key = root.get(request.getKey());
    
            if (request.getFieldType() == FieldType.DATE) {
                LocalDateTime dateValue = (LocalDateTime) value;
                Expression<LocalDateTime> keyAsLocalDateTime = key.as(LocalDateTime.class);
                return cb.and(cb.greaterThan(keyAsLocalDateTime, dateValue), predicate);
            }
    
            return cb.and(cb.greaterThan(key, (Comparable) value), predicate);
        }
    },
    
    GREATER_THAN_OR_EQUAL {
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, FilterRequest request, Predicate predicate) {
            Object value = request.getFieldType().parse(request.getValue().toString());
            Expression<? extends Comparable> key = root.get(request.getKey());
    
            if (request.getFieldType() == FieldType.DATE) {
                LocalDateTime startDate = (LocalDateTime) value;
                Expression<LocalDateTime> keyAsLocalDateTime = key.as(LocalDateTime.class);
                return cb.and(cb.greaterThanOrEqualTo(keyAsLocalDateTime, startDate), predicate);
            }
    
            return cb.and(cb.greaterThanOrEqualTo(key, (Comparable) value), predicate);
        }
    };    

    public abstract <T> Predicate build(Root<T> root, CriteriaBuilder cb, FilterRequest request, Predicate predicate);

}
