package de.gessnerfl.fakesmtp.model.query;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @Type(value = LogicalAnd.class, name = "and"),
        @Type(value = LogicalOr.class, name = "or"),
        @Type(value = Negation.class, name = "not"),
        @Type(value = UnaryExpression.class, name = "uexp"),
        @Type(value = BinaryExpression.class, name = "biexp"),
})
public interface LogicalExpression {
    <T> Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);
}
