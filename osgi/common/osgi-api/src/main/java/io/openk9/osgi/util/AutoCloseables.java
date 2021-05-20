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

package io.openk9.osgi.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;

public class AutoCloseables {

	public static AutoCloseableSafe mergeAutoCloseableToSafe(
		AutoCloseable autoCloseable) {

		return () -> {

			try {
				autoCloseable.close();
			}
			catch (Exception e) {
				_log.warn(e.getMessage(), e);
			}

		};
	}

	public static AutoCloseableSafe mergeAutoCloseableToSafe(
		AutoCloseable... autoCloseables) {

		return () -> {

			for (AutoCloseable autoCloseable : autoCloseables) {
				try {
					autoCloseable.close();
				}
				catch (Exception e) {
					_log.warn(e.getMessage(), e);
				}
			}


		};
	}

	public static AutoCloseableSafe mergeAutoCloseableToSafe(
		Collection<AutoCloseable> autoCloseables) {

		return () -> {

			for (AutoCloseable autoCloseable : autoCloseables) {
				try {
					autoCloseable.close();
				}
				catch (Exception e) {
					_log.warn(e.getMessage(), e);
				}
			}


		};
	}

	public interface AutoCloseableSafe {

		void close();

		default AutoCloseableSafe andThen(AutoCloseableSafe after) {
			Objects.requireNonNull(after);
			return () -> { close(); after.close(); };
		}

	}

	public static final AutoCloseableSafe NOTHING = () -> {};

	private static final Logger _log =
		LoggerFactory.getLogger(AutoCloseables.class);

}
