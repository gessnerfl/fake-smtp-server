package de.gessnerfl.fakesmtp.model.query;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.criteria.*;
import jakarta.validation.constraints.NotEmpty;

import static de.gessnerfl.fakesmtp.model.query.ExpressionValueHelper.convertDateIfApplicable;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
@JsonTypeName("equal")
public record EqualExpression(@NotEmpty String property, @NotEmpty Object value) implements FilterExpression {
    @Override
    public <T> Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Path<?> path = root.get(property);
        return cb.equal(path, convertDateIfApplicable(path, value));
    }
}
