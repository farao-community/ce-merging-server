/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.chain;

/**
 * see <a href="https://refactoring.guru/design-patterns/chain-of-responsibility">Chain of Responsibility</a>
 * @param <T> describes the object each handler works with
 */
public interface Handler<T> {

    boolean handle(final T request);
}
