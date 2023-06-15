package de.gessnerfl.fakesmtp.model.query;

import jakarta.persistence.criteria.Path;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExpressionValueHelper {
    public static Object convertDateIfApplicable(Path<?> path, Object value) {
        return path.getJavaType().isAssignableFrom(LocalDateTime.class) ? parseDate(value) : value;
    }

    private  static Object parseDate(Object value) {
        String dateString = value.toString();
        return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
