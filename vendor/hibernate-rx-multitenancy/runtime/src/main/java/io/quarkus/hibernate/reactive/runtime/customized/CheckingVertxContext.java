/*
 * Copyright (c) 2020-present SMC Treviso s.r.l. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.quarkus.hibernate.reactive.runtime.customized;

import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import org.hibernate.reactive.context.impl.VertxContext;

/**
 * The {@link VertxContext} in Hibernate Reactive is accessing the
 * Vert.x context directly, assuming this is the correct context as
 * intended by the developer, as Hibernate Reactive has no opinion in
 * regard to how Vert.x is integrated with other components.
 * The precise definition of "correct context" will most likely depend
 * on the runtime model and how other components are integrated with Vert.x;
 * in particular the lifecycle of the context needs to be specified.
 * For example in Quarkus's RestEasy Reactive we ensure that each request
 * will run on a separate context; this ensures operations relating to
 * different web requests are isolated among each other.
 * To ensure that Quarkus users are using Hibernate Reactive on a context
 * which is compatible with its expectations, this alternative implementation
 * of {@link VertxContext} actually checks on each context access if it's safe
 * to use by invoking {@link VertxContextSafetyToggle#validateContextIfExists(String, String)}.
 *
 * @see VertxContextSafetyToggle
 */
public final class CheckingVertxContext extends VertxContext {

    private static final String ERROR_MSG_ON_PROHIBITED_CONTEXT;
    private static final String ERROR_MSG_ON_UNKNOWN_CONTEXT;

    static {
        final String sharedmsg =
            " You can still use Hibernate Reactive, you just need to avoid using the methods which implicitly require accessing the stateful context, such as MutinySessionFactory#withTransaction and #withSession.";
        ERROR_MSG_ON_UNKNOWN_CONTEXT =
            "The current operation requires a safe (isolated) Vert.x sub-context, but the current context hasn't been flagged as such."
            + sharedmsg;
        ERROR_MSG_ON_PROHIBITED_CONTEXT =
            "The current Hibernate Reactive operation requires a safe (isolated) Vert.x sub-context, while the current context has been explicitly flagged as not compatible for this purpose."
            + sharedmsg;
    }

    @Override
    public <T> void put(Key<T> key, T instance) {
        VertxContextSafetyToggle.validateContextIfExists(
            ERROR_MSG_ON_PROHIBITED_CONTEXT,
            ERROR_MSG_ON_UNKNOWN_CONTEXT
        );
        super.put(key, instance);
    }

    @Override
    public <T> T get(Key<T> key) {
        VertxContextSafetyToggle.validateContextIfExists(
            ERROR_MSG_ON_PROHIBITED_CONTEXT,
            ERROR_MSG_ON_UNKNOWN_CONTEXT
        );
        return super.get(key);
    }

    @Override
    public void remove(Key<?> key) {
        VertxContextSafetyToggle.validateContextIfExists(
            ERROR_MSG_ON_PROHIBITED_CONTEXT,
            ERROR_MSG_ON_UNKNOWN_CONTEXT
        );
        super.remove(key);
    }

}
