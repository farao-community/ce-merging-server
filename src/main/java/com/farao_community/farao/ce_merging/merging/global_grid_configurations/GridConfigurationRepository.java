package com.farao_community.farao.ce_merging.merging.global_grid_configurations;

import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;

public interface GridConfigurationRepository<T> extends CrudRepository<T, String> {
    T findFirstByValidFromLessThanEqualAndValidToGreaterThanOrderByPublishedOnDesc(LocalDateTime date1, LocalDateTime date2);

    default T findLastPublishedValidBetween(final LocalDateTime date1, final LocalDateTime date2) {
        return findFirstByValidFromLessThanEqualAndValidToGreaterThanOrderByPublishedOnDesc(date1, date2);
    }
}
