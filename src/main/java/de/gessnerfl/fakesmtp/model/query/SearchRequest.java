package de.gessnerfl.fakesmtp.model.query;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

import java.util.Optional;

public class SearchRequest {
    public static SearchRequest of(LogicalExpression filter) {
        return SearchRequest.of(filter, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, null);
    }

    public static SearchRequest of(LogicalExpression filter, Sorting sort) {
        return SearchRequest.of(filter, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, sort);
    }

    public static SearchRequest of(LogicalExpression filter, int page, int size, Sorting sort) {
        final var req = new SearchRequest();
        req.setFilter(filter);
        req.setPage(page);
        req.setSize(size);
        req.setSort(sort);
        return req;
    }

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final String DEFAULT_SORT_PROPERTY = "receivedOn";

    private LogicalExpression filter;
    private int page = DEFAULT_PAGE;
    private int size = DEFAULT_PAGE_SIZE;
    private Sorting sort;

    public Optional<LogicalExpression> getFilter() {
        return Optional.ofNullable(filter);
    }

    public void setFilter(LogicalExpression filter) {
        this.filter = filter;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Optional<Sorting> getSort() {
        return Optional.ofNullable(sort);
    }

    public void setSort(Sorting sort) {
        this.sort = sort;
    }

    public Pageable getPageable() {
        Sort sort = getSort().map(this::mapSort).orElseGet(() -> Sort.by(Sort.Direction.DESC, DEFAULT_SORT_PROPERTY));
        return PageRequest.of(page, size, sort);
    }

    private Sort mapSort(Sorting sorting) {
        return Sort.by(sorting.getOrders().stream().map(this::mapSortOrder).toList());
    }

    private Sort.Order mapSortOrder(SortOrder o) {
        Assert.hasText(o.getProperty(), "property of sort order is missing");
        return new Sort.Order(o.getDirection() == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC, o.getProperty());
    }
}
