package de.gessnerfl.fakesmtp.model.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.util.Assert;

public class Negation implements LogicalExpression {
    private final LogicalExpression expression;

    @JsonCreator
    public Negation(@JsonProperty("expression") LogicalExpression expression) {
        this.expression = expression;
    }

    @Override
    public <T> Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Assert.notNull(expression, "expression of negation is missing");
        return cb.not(expression.toPredicate(root, query, cb));
    }

    public LogicalExpression getExpression() {
        return expression;
    }
}
