package de.gessnerfl.fakesmtp.model.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.criteria.*;
import org.springframework.util.Assert;

public class UnaryExpression implements LogicalExpression {
    private final String property;
    private final UnaryOperator operator;

    @JsonCreator
    public UnaryExpression(@JsonProperty("property") String property,
                           @JsonProperty("operator") UnaryOperator operator) {
        this.property = property;
        this.operator = operator;
    }

    @Override
    public <T> Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Assert.hasText(property, "property of binary expression missing");
        Assert.notNull(operator, "operator of binary expression missing");

        Path<?> path = root.get(property);
        return operator.build(path, cb);
    }

    public String getProperty() {
        return property;
    }

    public UnaryOperator getOperator() {
        return operator;
    }
}
