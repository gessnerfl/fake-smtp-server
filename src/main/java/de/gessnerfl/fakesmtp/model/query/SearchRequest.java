package de.gessnerfl.fakesmtp.model.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SearchRequest implements Serializable {

    private List<FilterRequest> filters;
    private List<SortRequest> sorts;
    private Integer page;
    private Integer size;
    private LogicalOperator logicalOperator;

    private SearchRequest() {
    }

    public List<FilterRequest> getFilters() {
        if (Objects.isNull(filters)) {
            return new ArrayList<>();
        }
        return filters;
    }

    public List<SortRequest> getSorts() {
        if (Objects.isNull(sorts)) {
            return new ArrayList<>();
        }
        return sorts;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getSize() {
        return size;
    }

    public LogicalOperator getLogicalOperator() {
        return logicalOperator;
    }

    public static SearchRequestBuilder builder() {
        return new SearchRequestBuilder();
    }

    public static class SearchRequestBuilder {
        private List<FilterRequest> filters;
        private List<SortRequest> sorts;
        private Integer page;
        private Integer size;
        private LogicalOperator logicalOperator;

        private SearchRequestBuilder() {
        }

        public SearchRequestBuilder filters(List<FilterRequest> filters) {
            this.filters = filters;
            return this;
        }

        public SearchRequestBuilder sorts(List<SortRequest> sorts) {
            this.sorts = sorts;
            return this;
        }

        public SearchRequestBuilder page(Integer page) {
            this.page = page;
            return this;
        }

        public SearchRequestBuilder size(Integer size) {
            this.size = size;
            return this;
        }

        public SearchRequestBuilder logicalOperator(LogicalOperator logicalOperator) {
            this.logicalOperator = logicalOperator;
            return this;
        }

        public SearchRequest build() {
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.filters = filters;
            searchRequest.sorts = sorts;
            searchRequest.page = page;
            searchRequest.size = size;
            searchRequest.logicalOperator = logicalOperator;
            return searchRequest;
        }
    }

    public void addFilter(FilterRequest filter) {
        if (filters == null) {
            filters = new ArrayList<>();
        }
        filters.add(filter);
    }

}
