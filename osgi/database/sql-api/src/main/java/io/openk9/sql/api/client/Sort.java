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

package io.openk9.sql.api.client;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Sort {

	public static Sort asc(String column) {
		return new Sort(column, Order.ASC);
	}

	public static Sort desc(String column) {
		return new Sort(column, Order.ASC);
	}

	public static Sort of(String column) {
		return new Sort(column, Order.UNORDERED);
	}

	private final String column;
	private final Order order;

	public static final Sort DEFAULT =
		new Sort(null, Order.UNORDERED);
}
