package de.gessnerfl.fakesmtp.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

public class EmailSpecificationsBuilder {
    
    private final List<SpecSearchCriteria> params;

    public EmailSpecificationsBuilder() {
        params = new ArrayList<>();
    }

    public final EmailSpecificationsBuilder with(String key, String operation, Object value, 
      String prefix, String suffix) {
        return with(null, key, operation, value, prefix, suffix);
    }

    public final EmailSpecificationsBuilder with(String orPredicate, String key, String operation, 
      Object value, String prefix, String suffix) {
        SearchOperation searchOperation = SearchOperation.getSimpleOperation(operation.charAt(0));
        if (searchOperation != null) {
            if (searchOperation == SearchOperation.EQUALITY) {
                boolean startWithAsterisk = prefix != null && 
                  prefix.contains(SearchOperation.ZERO_OR_MORE_REGEX);
                boolean endWithAsterisk = suffix != null && 
                  suffix.contains(SearchOperation.ZERO_OR_MORE_REGEX);

                if (startWithAsterisk && endWithAsterisk) {
                    searchOperation = SearchOperation.CONTAINS;
                } else if (startWithAsterisk) {
                    searchOperation = SearchOperation.ENDS_WITH;
                } else if (endWithAsterisk) {
                    searchOperation = SearchOperation.STARTS_WITH;
                }
            }
            params.add(new SpecSearchCriteria(orPredicate, key, searchOperation, value));
        }
        return this;
    }

    public Specification<Email> build() {
        if (params.size() == 0)
            return null;

        Specification<Email> result = new EmailSpecification(params.get(0));
     
        for (int i = 1; i < params.size(); i++) {
            result = params.get(i).isOrPredicate()
              ? Specification.where(result).or(new EmailSpecification(params.get(i))) 
              : Specification.where(result).and(new EmailSpecification(params.get(i)));
        }
        
        return result;
    }    
}
