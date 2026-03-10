/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.abstraction;

import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;

public interface GridConfigurationRepository<T> extends CrudRepository<T, String> {
    T findFirstByValidFromLessThanEqualAndValidToGreaterThanOrderByPublishedOnDesc(LocalDateTime date1, LocalDateTime date2);

    default T findLastPublishedValid(final LocalDateTime validityDate) {
        return findFirstByValidFromLessThanEqualAndValidToGreaterThanOrderByPublishedOnDesc(validityDate, validityDate);
    }
}
