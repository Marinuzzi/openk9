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

package io.openk9.entity.manager.cleaner;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;


@ApplicationScoped
public class OrganizationEntityNameCleaner extends DefaultEntityNameCleaner {

	@Inject
	@ConfigProperty(
		name = "openk9.entity.cleaner.stop-words",
		defaultValue = "spa,s.p.a.,srl,s.r.l.,s.r.l,s.p.a"
	)
	String[] _stopWords;

	@Override
	public String getEntityType() {
		return "organization";
	}

	@Override
	public QueryBuilder cleanEntityName(String tenantId, String entityName) {
		return super.cleanEntityName(tenantId, entityName);
	}

	@Override
	public String cleanEntityName(String entityName) {

		for (String stopWord : _stopWords) {
			entityName = entityName.replaceAll(stopWord, "");
		}

		return super.cleanEntityName(entityName);
	}

	@Override
	protected QueryBuilder createQueryBuilder(String entityName) {

		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

		boolQueryBuilder.must(
			QueryBuilders.matchQuery("name", entityName)
		);

		boolQueryBuilder.must(
			QueryBuilders.matchQuery("type.keyword", getEntityType())
		);

		return boolQueryBuilder;

	}

}
