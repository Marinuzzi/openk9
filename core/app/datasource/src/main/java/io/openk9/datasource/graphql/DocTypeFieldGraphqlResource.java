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
import io.openk9.common.util.FieldValidator;
import io.openk9.common.util.Response;
import io.openk9.common.util.SortBy;
import io.openk9.datasource.model.Analyzer;
import io.openk9.datasource.model.DocTypeField;
import io.openk9.datasource.model.Tab;
import io.openk9.datasource.model.dto.DocTypeFieldDTO;
import io.openk9.datasource.model.dto.TranslationDTO;
import io.openk9.datasource.service.DocTypeFieldService;
import io.openk9.datasource.service.TranslationService;
import io.openk9.datasource.service.util.Tuple2;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;

import java.util.List;
import java.util.Set;

@GraphQLApi
@ApplicationScoped
@CircuitBreaker
public class DocTypeFieldGraphqlResource {

	@Query
	public Uni<Connection<DocTypeField>> getDocTypeFields(
		@Description("fetching only nodes after this node (exclusive)") String after,
		@Description("fetching only nodes before this node (exclusive)") String before,
		@Description("fetching only the first certain number of nodes") Integer first,
		@Description("fetching only the last certain number of nodes") Integer last,
		String searchText, Set<SortBy> sortByList) {
		return docTypeFieldService.findConnection(
			after, before, first, last, searchText, sortByList);
	}

	@Query
	public Uni<Connection<DocTypeField>> getDocTypeFieldsByParent(
		@Description("fetching only nodes after this node (exclusive)") String after,
		@Description("fetching only nodes before this node (exclusive)") String before,
		@Description("fetching only the first certain number of nodes") Integer first,
		@Description("fetching only the last certain number of nodes") Integer last,
		long parentId, String searchText, Set<SortBy> sortByList) {
		return docTypeFieldService.findConnection(
			parentId, after, before, first, last, searchText, sortByList);
	}

	public Uni<DocTypeField> parent(
		@Source DocTypeField docTypeField) {
		return docTypeFieldService.getParent(docTypeField);
	}

	public Uni<Connection<DocTypeField>> subFields(
		@Source DocTypeField docTypeField,
		@Description("fetching only nodes after this node (exclusive)") String after,
		@Description("fetching only nodes before this node (exclusive)") String before,
		@Description("fetching only the first certain number of nodes") Integer first,
		@Description("fetching only the last certain number of nodes") Integer last,
		String searchText, Set<SortBy> sortByList,
		@Description("if notEqual is true, it returns unbound entities") @DefaultValue("false") boolean notEqual) {

		return docTypeFieldService.getSubDocTypeFields(
			docTypeField, after, before, first, last, searchText, sortByList,
			notEqual);
	}


	public Uni<Analyzer> analyzer(@Source DocTypeField docTypeField) {
		return docTypeFieldService.getAnalyzer(docTypeField.getId());
	}

	@Mutation
	public Uni<Response<DocTypeField>> createSubField(
		@Id long parentDocTypeFieldId, DocTypeFieldDTO docTypeFieldDTO) {

		return Uni.createFrom().deferred(() -> {

			List<FieldValidator> validatorList =
				docTypeFieldService.getValidator().validate(docTypeFieldDTO);

			if (validatorList.isEmpty()) {

				return docTypeFieldService
					.createSubField(parentDocTypeFieldId, docTypeFieldDTO)
					.onItemOrFailure()
					.transform((e, t) -> {

						List<FieldValidator> newValidatorList = null;

						if (t != null) {
							newValidatorList = List.of(
								FieldValidator.of("fieldName", t.getMessage()));
						}

						return Response.of(e, newValidatorList);

					});
			}

			return Uni.createFrom().item(Response.of(null, validatorList));
		});

	}

	@Mutation
	public Uni<Tuple2<DocTypeField, Analyzer>> bindAnalyzerToDocTypeField(
		@Id long docTypeFieldId, @Id long analyzerId) {
		return docTypeFieldService.bindAnalyzer(docTypeFieldId, analyzerId);
	}

	@Mutation
	public Uni<Tuple2<DocTypeField, Analyzer>> unbindAnalyzerFromDocTypeField(
		@Id long docTypeFieldId) {
		return docTypeFieldService.unbindAnalyzer(docTypeFieldId);
	}

	public Uni<Set<TranslationDTO>> getTranslations(@Source DocTypeField docTypeField) {
		return translationService.getTranslationDTOs(DocTypeField.class, docTypeField.getId());
	}

	@Mutation
	public Uni<Tuple2<String, String>> addDocTypeFieldTranslation(
		@Id @Name("docTypeFieldId") long docTypeFieldId,
		String language, String key, String value) {

		return translationService
			.addTranslation(DocTypeField.class, docTypeFieldId, language, key, value)
			.map((__) -> Tuple2.of("ok", null));
	}

	@Mutation
	public Uni<Tuple2<String, String>> deleteDocTypeFieldTranslation(
		@Id @Name("docTypeFieldId") long docTypeFieldId,
		String language, String key) {

		return translationService
			.deleteTranslation(Tab.class, docTypeFieldId, language, key)
			.map((__) -> Tuple2.of("ok", null));
	}

	@Inject
	DocTypeFieldService docTypeFieldService;

	@Inject
	TranslationService translationService;
}
