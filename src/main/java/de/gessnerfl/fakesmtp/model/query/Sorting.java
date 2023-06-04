package de.gessnerfl.fakesmtp.model.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Sorting {
    private final List<SortOrder> orders;

    public static Sorting by(SortOrder...orders){
        return new Sorting(Arrays.asList(orders));
    }


    @JsonCreator
    public Sorting(@JsonProperty("orders") List<SortOrder> orders) {
        this.orders = orders != null ? orders : new ArrayList<>();
    }

    public List<SortOrder> getOrders() {
        return orders;
    }
}
