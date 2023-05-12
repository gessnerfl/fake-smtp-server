package de.gessnerfl.fakesmtp.model.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest implements Serializable {

    private List<FilterRequest> filters;
    private List<SortRequest> sorts;
    private Integer page;
    private Integer size;
    private LogicalOperator logicalOperator;

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

}
