package de.gessnerfl.fakesmtp.model;

import java.util.Set;

import javax.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

public class EmailSpecification {

    public static Specification<Email> textInAllColumns(String text, Set<String> fields) {
        if (!text.contains("%")) {
            text = "%" + text + "%";
        }
        final String finalText = text;

        return (root, query, builder) -> builder
                .or(root.getModel().getDeclaredSingularAttributes().stream().filter(a -> {
                    return fields.contains(a.getName());
                }).map(a -> builder.like(builder.lower(root.get(a.getName())), finalText.toLowerCase())).toArray(Predicate[]::new));
    }
    
}
