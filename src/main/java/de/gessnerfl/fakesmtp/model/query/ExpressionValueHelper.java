package de.gessnerfl.fakesmtp.model.query;

import jakarta.persistence.criteria.Path;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ExpressionValueHelper {
    private ExpressionValueHelper(){}

    public static Object convertDateIfApplicable(Path<?> path, Object value) {
        return path.getJavaType().isAssignableFrom(ZonedDateTime.class) ? parseDate(value) : value;
    }

    private  static Object parseDate(Object value) {
        String dateString = value.toString();
        return ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
