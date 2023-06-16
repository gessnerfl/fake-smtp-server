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
        @Type(value = IsNullExpression.class, name = "is_null"),
        @Type(value = IsNotNullExpression.class, name = "is_not_null"),
        @Type(value = EqualExpression.class, name = "equal"),
        @Type(value = NotEqualExpression.class, name = "not_equal"),
        @Type(value = LikeExpression.class, name = "like"),
        @Type(value = LessThanExpression.class, name = "less_than"),
        @Type(value = LessThanOrEqualExpression.class, name = "less_than_or_equal"),
        @Type(value = GreaterThanExpression.class, name = "greater_than"),
        @Type(value = GreaterThanOrEqualExpression.class, name = "greater_than_or_equal"),
})
public interface FilterExpression {
    <T> Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);
}
