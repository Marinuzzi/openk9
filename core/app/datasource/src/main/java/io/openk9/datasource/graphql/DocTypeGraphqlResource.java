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

import java.util.List;
import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.openk9.common.graphql.util.relay.Connection;
import io.openk9.common.util.FieldValidator;
import io.openk9.common.util.Response;
import io.openk9.common.util.SortBy;
import io.openk9.datasource.model.DocType;
import io.openk9.datasource.model.DocTypeField;
import io.openk9.datasource.model.DocTypeTemplate;
import io.openk9.datasource.model.dto.base.DocTypeDTO;
import io.openk9.datasource.model.dto.base.DocTypeFieldDTO;
import io.openk9.datasource.model.dto.request.DocTypeWithTemplateDTO;
import io.openk9.datasource.service.DocTypeFieldService;
import io.openk9.datasource.service.DocTypeService;
import io.openk9.datasource.service.util.K9EntityEvent;
import io.openk9.datasource.service.util.Tuple2;

import io.smallrye.graphql.api.Subscription;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;
import org.hibernate.reactive.mutiny.Mutiny;

@GraphQLApi
@ApplicationScoped
@CircuitBreaker
public class DocTypeGraphqlResource {

	@Query
	public Uni<Connection<DocType>> getDocTypes(
		@Description("fetching only nodes after this node (exclusive)") String after,
		@Description("fetching only nodes before this node (exclusive)") String before, 
		@Description("fetching only the first certain number of nodes") Integer first, 
		@Description("fetching only the last certain number of nodes") Integer last,
		String searchText, Set<SortBy> sortByList) {
		return docTypeService.findConnection(
			after, before, first, last, searchText, sortByList);
	}

	public Uni<Connection<DocTypeField>> docTypeFields(
		@Source DocType docType,
		@Description("fetching only nodes after this node (exclusive)") String after,
		@Description("fetching only nodes before this node (exclusive)") String before,
		@Description("fetching only the first certain number of nodes") Integer first,
		@Description("fetching only the last certain number of nodes") Integer last,
		String searchText, Set<SortBy> sortByList,
		@Description("if notEqual is true, it returns unbound entities") @DefaultValue("false") boolean notEqual) {
		return getDocTypeFieldsFromDocType(
			docType.getId(), after, before, first, last, searchText, sortByList,
			notEqual);
	}

	@Query
	public Uni<Connection<DocTypeField>> getDocTypeFieldsFromDocType(
		@Id long docTypeId,
		@Description("fetching only nodes after this node (exclusive)") String after,
		@Description("fetching only nodes before this node (exclusive)") String before,
		@Description("fetching only the first certain number of nodes") Integer first,
		@Description("fetching only the last certain number of nodes") Integer last,
		String searchText, Set<SortBy> sortByList,
		@Description("if notEqual is true, it returns unbound entities") @DefaultValue("false") boolean notEqual) {
		return docTypeService.getDocTypeFieldsConnection(
			docTypeId, after, before, first, last, searchText, sortByList, notEqual);
	}

	@Query
	public Uni<Connection<DocTypeField>> getDocTypeFieldsFromDocTypeByParent(
		@Id long docTypeId,
		@Description("id of the parent docTypeField (0 if root )") long parentId,
		@Description("fetching only nodes after this node (exclusive)") String after,
		@Description("fetching only nodes before this node (exclusive)") String before,
		@Description("fetching only the first certain number of nodes") Integer first,
		@Description("fetching only the last certain number of nodes") Integer last,
		String searchText, Set<SortBy> sortByList,
		@Description("if notEqual is true, it returns unbound entities") @DefaultValue("false") boolean notEqual) {
		return docTypeService.getDocTypeFieldsConnectionByParent(
			docTypeId, parentId, after, before, first, last, searchText, sortByList, notEqual);
	}

	@Query
	public Uni<DocType> getDocType(@Id long id) {
		return docTypeService.findById(id);
	}

	@Query
	public Uni<DocTypeField> getDocTypeField(@Id long id) {
		return docTypeFieldService.findById(id);
	}

	public Uni<DocType> docType(@Source DocTypeField docTypeField) {
		return sessionFactory.withTransaction(s -> s
			.merge(docTypeField)
			.flatMap(merged -> s.fetch(merged.getDocType()))
		);
	}

	public Uni<DocTypeTemplate> docTypeTemplate(@Source DocType docType) {
		return sessionFactory.withTransaction(s -> s
			.merge(docType)
			.flatMap(merged -> s.fetch(merged.getDocTypeTemplate()))
		);
	}

	public Uni<Response<DocType>> patchDocType(@Id long id, DocTypeDTO docTypeDTO) {
		return docTypeService.getValidator().patch(id, docTypeDTO);
	}

	public Uni<Response<DocType>> updateDocType(@Id long id, DocTypeDTO docTypeDTO) {
		return docTypeService.getValidator().update(id, docTypeDTO);
	}

	public Uni<Response<DocType>> createDocType(DocTypeDTO docTypeDTO) {
		return docTypeService.getValidator().create(docTypeDTO);
	}

	@Mutation
	public Uni<Response<DocType>> docType(
		@Id Long id, DocTypeDTO docTypeDTO,
		@DefaultValue("false") boolean patch) {

		if (id == null) {
			return createDocType(docTypeDTO);
		} else {
			return patch
				? patchDocType(id, docTypeDTO)
				: updateDocType(id, docTypeDTO);
		}

	}

	@Mutation
	public Uni<Response<DocType>> docTypeWithTemplate(
		@Id Long id, DocTypeWithTemplateDTO docTypeWithTemplateDTO,
		@DefaultValue("false") boolean patch) {

		if (id == null) {
			return createDocType(docTypeWithTemplateDTO);
		}
		else {
			return patch
				? patchDocType(id, docTypeWithTemplateDTO)
				: updateDocType(id, docTypeWithTemplateDTO);
		}

	}

	@Mutation
	public Uni<DocType> deleteDocType(@Id long docTypeId) {
		return docTypeService.deleteById(docTypeId);
	}

	@Mutation
	public Uni<Response<DocTypeField>> docTypeField(
		@Id long docTypeId, @Id Long docTypeFieldId, DocTypeFieldDTO docTypeFieldDTO,
		@DefaultValue("false") boolean patch) {

		return Uni.createFrom().deferred(() -> {

			List<FieldValidator> validatorList =
				docTypeFieldService.getValidator().validate(docTypeFieldDTO);

			if (validatorList.isEmpty()) {

				if (docTypeFieldId == null) {
					return docTypeService.addDocTypeField(docTypeId, docTypeFieldDTO)
						.map(e -> Response.of(e.right, null));
				} else {
					return (
						patch
							? docTypeFieldService.patch(docTypeFieldId, docTypeFieldDTO)
							: docTypeFieldService.update(docTypeFieldId, docTypeFieldDTO)
					).map(e -> Response.of(e, null));
				}

			}

			return Uni.createFrom().item(Response.of(null, validatorList));
		});

	}

	@Mutation
	public Uni<Tuple2<DocType, Long>> removeDocTypeField(
		@Id long docTypeId, @Id long docTypeFieldId) {
		return docTypeService.removeDocTypeField(docTypeId, docTypeFieldId);
	}

	@Mutation
	public Uni<Tuple2<DocType, DocTypeTemplate>> bindDocTypeToDocTypeTemplate(
		@Id @Name("docTypeId") long docTypeId,
		@Id @Name("docTypeTemplateId") long docTypeTemplateId) {
		return docTypeService.setDocTypeTemplate(docTypeId, docTypeTemplateId);
	}

	@Mutation
	public Uni<DocType> unbindDocTypeTemplateFromDocType(
		@Id @Name("docTypeId") long docTypeId) {
		return docTypeService.unsetDocType(docTypeId);
	}

	@Subscription
	public Multi<DocType> docTypeCreated() {
		return docTypeService
			.getProcessor()
			.filter(K9EntityEvent::isCreate)
			.map(K9EntityEvent::getEntity);
	}

	@Subscription
	public Multi<DocType> docTypeDeleted() {
		return docTypeService
			.getProcessor()
			.filter(K9EntityEvent::isDelete)
			.map(K9EntityEvent::getEntity);
	}

	@Subscription
	public Multi<DocType> docTypeUpdated() {
		return docTypeService
			.getProcessor()
			.filter(K9EntityEvent::isUpdate)
			.map(K9EntityEvent::getEntity);
	}

	@Inject
	DocTypeService docTypeService;

	@Inject
	Mutiny.SessionFactory sessionFactory;

	@Inject
	DocTypeFieldService docTypeFieldService;

}