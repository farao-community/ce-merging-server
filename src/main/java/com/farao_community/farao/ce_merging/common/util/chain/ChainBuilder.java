/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util.chain;

public final class ChainBuilder<T> {

    public static <T> ChainBuilder<T> chainBuilder() {
        return new ChainBuilder<>();
    }

    private HandlerImpl<T> first;

    private ChainBuilder() {
    }

    public SuccessorBuilder first(final Handler<T> handler) {
        first = new HandlerImpl<>(handler);
        return new SuccessorBuilder(first);
    }

    @SuppressWarnings("PublicInnerClass")
    public final class SuccessorBuilder {

        private HandlerImpl<T> current;

        private SuccessorBuilder(final HandlerImpl<T> current) {
            this.current = current;
        }

        public SuccessorBuilder next(final Handler<T> next) {
            final HandlerImpl<T> successorWrapper = new HandlerImpl<>(next);
            current.setNext(successorWrapper);
            current = successorWrapper;
            return this;
        }

        public Chain<T> build() {
            return new ChainImpl<>(first);
        }
    }

    private static final class ChainImpl<T> implements Chain<T> {

        private final Handler<T> first;

        private ChainImpl(final Handler<T> first) {
            this.first = first;
        }

        @Override
        public void handle(final T t) {
            first.handle(t);
        }
    }

    private static final class HandlerImpl<T> implements Handler<T> {

        private final Handler<T> delegate;
        private       Handler<T> next;

        private HandlerImpl(final Handler<T> delegate) {
            this.delegate = delegate;
        }

        private void setNext(final HandlerImpl<T> next) {
            this.next = next;
        }

        @Override
        public boolean handle(final T t) {
            return delegate.handle(t) ||
                       next != null && next.handle(t);
        }
    }
}
