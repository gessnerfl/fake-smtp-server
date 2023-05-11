package de.gessnerfl.fakesmtp.model.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SortRequest implements Serializable {
    
    private String key;

    private SortDirection direction;

}
