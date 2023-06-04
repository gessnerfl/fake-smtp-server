package de.gessnerfl.fakesmtp.model.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;

public class LogicalAnd implements LogicalExpression {

    public static LogicalAnd of(LogicalExpression...expressions){
        return new LogicalAnd(Arrays.asList(expressions));
    }

    private final List<LogicalExpression> expressions;

    @JsonCreator
    public LogicalAnd(@JsonProperty("expressions") List<LogicalExpression> expressions) {
        this.expressions = expressions;
    }

    @Override
    public <T> Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Assert.notEmpty(expressions, "expression of logical and is missing");
        return cb.and(expressions.stream().map(e -> e.toPredicate(root, query, cb)).toArray(Predicate[]::new));
    }

    public List<LogicalExpression> getExpressions() {
        return expressions;
    }
}
