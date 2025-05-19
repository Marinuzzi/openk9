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

package io.openk9.datasource.graphql;

import io.openk9.common.graphql.util.relay.Connection;
import io.openk9.common.util.Response;
import io.openk9.common.util.SortBy;
import io.openk9.datasource.index.response.CatResponse;
import io.openk9.datasource.model.DataIndex;
import io.openk9.datasource.model.Datasource;
import io.openk9.datasource.model.DocType;
import io.openk9.datasource.model.DocTypeField;
import io.openk9.datasource.model.dto.base.DataIndexDTO;
import io.openk9.datasource.service.DataIndexService;
import io.openk9.datasource.service.util.K9EntityEvent;
import io.openk9.datasource.service.util.Tuple2;
import io.smallrye.graphql.api.Subscription;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;

import java.util.Set;

@GraphQLApi
@ApplicationScoped
@CircuitBreaker
public class DataIndexGraphqlResource {

	@Inject
	DataIndexService dataIndexService;

	@Mutation
	public Uni<Tuple2<DataIndex, DocType>> addDocTypeToDataIndex(@Id long dataIndexId, @Id long docTypeId) {
		return dataIndexService.addDocType(dataIndexId, docTypeId);
	}

	@Mutation
	public Uni<Response<DataIndex>> dataIndex(
		@Id long datasourceId, DataIndexDTO dataIndexDTO) {

		return dataIndexService.create(datasourceId, dataIndexDTO);
	}

	@Subscription
	public Multi<DataIndex> dataIndexCreated() {
		return dataIndexService
			.getProcessor()
			.filter(K9EntityEvent::isCreate)
			.map(K9EntityEvent::getEntity);
	}

	@Subscription
	public Multi<DataIndex> dataIndexDeleted() {
		return dataIndexService
			.getProcessor()
			.filter(K9EntityEvent::isDelete)
			.map(K9EntityEvent::getEntity);
	}

	@Subscription
	public Multi<DataIndex> dataIndexUpdated() {
		return dataIndexService
			.getProcessor()
			.filter(K9EntityEvent::isUpdate)
			.map(K9EntityEvent::getEntity);
	}

	@Mutation
	public Uni<DataIndex> deleteDataIndex(@Id long dataIndexId) {
		return dataIndexService.deleteById(dataIndexId);
	}

	public Uni<Connection<DocType>> docTypes(
		@Source DataIndex dataIndex,
		@Description("fetching only nodes after this node (exclusive)") String after,
		@Description("fetching only nodes before this node (exclusive)") String before,
		@Description("fetching only the first certain number of nodes") Integer first,
		@Description("fetching only the last certain number of nodes") Integer last,
		String searchText, Set<SortBy> sortByList,
		@Description("if notEqual is true, it returns unbound entities") @DefaultValue("false") boolean notEqual) {

		return dataIndexService.getDocTypesConnection(
			dataIndex.getId(), after, before, first, last, searchText, sortByList,
			notEqual);
	}

	public Uni<CatResponse> getCat(@Source DataIndex dataIndex){
		return dataIndexService.catIndex(dataIndex.getId());
	}

	@Query
	public Uni<DataIndex> getDataIndex(@Id long id) {
		return dataIndexService.findById(id);
	}

	@Query
	public Uni<Connection<DataIndex>> getDataIndices(
		@Description("fetching only nodes after this node (exclusive)") String after,
		@Description("fetching only nodes before this node (exclusive)") String before,
		@Description("fetching only the first certain number of nodes") Integer first,
		@Description("fetching only the last certain number of nodes") Integer last,
		String searchText, Set<SortBy> sortByList) {
		return dataIndexService.findConnection(
			after, before, first, last, searchText, sortByList);
	}

	public Uni<Datasource> getDatasource(@Source DataIndex dataIndex){
		return dataIndexService.datasource(dataIndex.getId());
	}

	public Uni<Long> getDocCount(@Source DataIndex dataIndex) {
		return dataIndexService.getCountIndexDocuments(dataIndex.getId());
	}

	public Uni<DocTypeField> getEmbeddingDocTypeField(@Source DataIndex dataIndex) {
		return dataIndexService.getEmbeddingDocTypeField(dataIndex.getId());
	}

	public Uni<String> mappings(@Source DataIndex dataIndex) {
		return dataIndexService
			.getMappings(dataIndex.getId());
	}

	@Mutation
	public Uni<Tuple2<DataIndex, DocType>> removeDocTypeFromDataIndex(@Id long dataIndexId, @Id long docTypeId) {
		return dataIndexService.removeDocType(dataIndexId, docTypeId);
	}

	public Uni<String> settings(@Source DataIndex dataIndex) {
		return dataIndexService
			.getSettings(dataIndex.getId());
	}

}