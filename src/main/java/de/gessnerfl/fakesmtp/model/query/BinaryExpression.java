package de.gessnerfl.fakesmtp.model.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.criteria.*;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BinaryExpression implements LogicalExpression {
    private final String property;
    private final BinaryOperator operator;
    private final Object value;

    @JsonCreator
    public BinaryExpression(@JsonProperty("property") String property,
                            @JsonProperty("operator") BinaryOperator operator,
                            @JsonProperty("value") Object value) {
        this.property = property;
        this.operator = operator;
        this.value = value;
    }

    @Override
    public <T> Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Assert.hasText(property, "property of binary expression missing");
        Assert.notNull(operator, "operator of binary expression missing");
        Assert.notNull(value, "value of binary expression missing");

        Path<?> path = root.get(property);
        Object value = path.getJavaType().isAssignableFrom(LocalDateTime.class) ? parseDate(this.value) : this.value;
        return operator.build(path, cb, value);
    }

    private Object parseDate(Object value) {
        String dateString = value.toString();
        return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public String getProperty() {
        return property;
    }

    public BinaryOperator getOperator() {
        return operator;
    }

    public Object getValue() {
        return value;
    }
}
