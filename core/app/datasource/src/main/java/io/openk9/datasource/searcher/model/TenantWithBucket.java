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

package io.openk9.datasource.searcher.model;

import java.util.HashSet;

import io.openk9.api.tenantmanager.TenantManager;
import io.openk9.datasource.index.model.IndexName;
import io.openk9.datasource.model.Bucket;
import io.openk9.datasource.model.Datasource;

import lombok.Getter;

@Getter
public class TenantWithBucket {

	private final TenantManager.Tenant tenant;
	private final Bucket bucket;
	private final String[] indexNames;

	public TenantWithBucket(TenantManager.Tenant tenant, Bucket bucket) {

		this.tenant = tenant;
		this.bucket = bucket;

		var tenantId = this.tenant.schemaName();
		var datasources = this.bucket.getDatasources();

		var indexNameSet = new HashSet<String>();
		for (Datasource datasource : datasources) {
			var dataIndex = datasource.getDataIndex();

			indexNameSet.add(IndexName.from(tenantId, dataIndex).toString());
		}

		this.indexNames = indexNameSet.toArray(String[]::new);

	}

}
