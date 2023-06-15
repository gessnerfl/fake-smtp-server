package de.gessnerfl.fakesmtp.model.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;

public class SortOrder {
    @NotEmpty
    private String property;
    private SortDirection direction;

    public SortOrder() {
    }

    public SortOrder(String property) {
        this(property, SortDirection.ASC);
    }

    public SortOrder(String property, SortDirection direction) {
        this.property = property;
        this.direction = direction;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public SortDirection getDirection() {
        return direction != null ? direction : SortDirection.ASC;
    }

    public void setDirection(SortDirection direction) {
        this.direction = direction;
    }

}
