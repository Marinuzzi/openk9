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

package io.openk9.datasource.service;

import io.openk9.common.graphql.util.relay.Connection;
import io.openk9.common.util.FieldValidator;
import io.openk9.common.util.Response;
import io.openk9.common.util.SortBy;
import io.openk9.datasource.graphql.dto.PluginWithDocTypeDTO;
import io.openk9.datasource.mapper.PluginDriverMapper;
import io.openk9.datasource.model.AclMapping;
import io.openk9.datasource.model.AclMapping_;
import io.openk9.datasource.model.DocTypeField;
import io.openk9.datasource.model.PluginDriver;
import io.openk9.datasource.model.PluginDriverDocTypeFieldKey;
import io.openk9.datasource.model.PluginDriverDocTypeFieldKey_;
import io.openk9.datasource.model.PluginDriver_;
import io.openk9.datasource.model.UserField;
import io.openk9.datasource.model.dto.PluginDriverDTO;
import io.openk9.datasource.model.util.K9Entity_;
import io.openk9.datasource.resource.util.Filter;
import io.openk9.datasource.resource.util.Page;
import io.openk9.datasource.resource.util.Pageable;
import io.openk9.datasource.service.util.BaseK9EntityService;
import io.openk9.datasource.service.util.Tuple2;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;
import javax.validation.ConstraintViolation;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class PluginDriverService
	extends BaseK9EntityService<PluginDriver, PluginDriverDTO> {
	@Inject
	DocTypeFieldService docTypeFieldService;

	PluginDriverService(PluginDriverMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public Class<PluginDriver> getEntityClass() {
		return PluginDriver.class;
	}

	@Override
	public String[] getSearchFields() {
		return new String[]{
			PluginDriver_.NAME, PluginDriver_.DESCRIPTION, PluginDriver_.TYPE
		};
	}

	public Uni<Connection<DocTypeField>> getDocTypeFieldsConnection(
		long pluginDriverId, String after,
		String before, Integer first, Integer last, String searchText,
		Set<SortBy> sortByList, boolean notEqual) {

		return findJoinConnection(
			pluginDriverId, PluginDriver_.ACL_MAPPINGS, DocTypeField.class,
			docTypeFieldService.getSearchFields(), after, before, first,
			last, searchText, sortByList, notEqual, pluginDriverRoot ->
				pluginDriverRoot
					.join(PluginDriver_.aclMappings)
					.join(AclMapping_.docTypeField),
			(cb, pluginDriverRoot) ->
				pluginDriverRoot
					.getJoins()
					.stream()
					.filter(e -> Objects.equals(
						e.getAttribute(),
						PluginDriver_.aclMappings
					))
					.map(e -> (Join<PluginDriver, AclMapping>) e)
					.map(e -> e.get(AclMapping_.userField))
					.map(cb::asc)
					.collect(Collectors.toList())
		);

	}

	public Uni<Page<DocTypeField>> getDocTypeFields(
		long pluginDriverId, Pageable pageable) {
		return getDocTypeFields(pluginDriverId, pageable, Filter.DEFAULT);
	}

	public Uni<Page<DocTypeField>> getDocTypeFields(
		long pluginDriverId, Pageable pageable, String searchText) {

		return findAllPaginatedJoin(
			new Long[]{pluginDriverId},
			PluginDriver_.ACL_MAPPINGS, DocTypeField.class,
			pageable.getLimit(), pageable.getSortBy().name(),
			pageable.getAfterId(), pageable.getBeforeId(),
			searchText
		);
	}

	public Uni<Set<DocTypeField>> getDocTypeFieldsInPluginDriver(
		long pluginDriverId) {

		return sessionFactory.withTransaction(
			s ->
				findById(s, pluginDriverId)
					.flatMap(
						ep -> s.fetch(ep.getAclMappings()))
					.map(l -> l
						.stream()
						.map(AclMapping::getDocTypeField)
						.collect(Collectors.toSet()))
		);

	}

	public Uni<List<DocTypeField>> getDocTypeFieldsNotInPluginDriver(
		long pluginDriverId) {

		return sessionFactory.withTransaction(
			s -> {

				CriteriaBuilder cb = sessionFactory.getCriteriaBuilder();

				CriteriaQuery<DocTypeField> query =
					cb.createQuery(DocTypeField.class);

				Root<DocTypeField> root = query.from(DocTypeField.class);

				Subquery<Long> subquery = query.subquery(Long.class);

				Root<PluginDriver> from = subquery.from(PluginDriver.class);

				SetJoin<PluginDriver, AclMapping> rootJoin =
					from.joinSet(PluginDriver_.ACL_MAPPINGS);

				Path<PluginDriverDocTypeFieldKey> pluginDriverDocTypeFieldKeyPath =
					rootJoin.get(AclMapping_.key);

				Path<Long> docTypeFieldId = pluginDriverDocTypeFieldKeyPath.get(
					PluginDriverDocTypeFieldKey_.docTypeFieldId);

				subquery.select(docTypeFieldId);

				subquery.where(
					cb.equal(from.get(K9Entity_.id), pluginDriverId));

				query.where(
					cb.in(root.get(K9Entity_.id)).value(subquery).not());

				return s.createQuery(query).getResultList();

			}
		);
	}

	public Uni<Page<DocTypeField>> getDocTypeFields(
		long pluginDriverId, Pageable pageable,
		Filter filter) {

		return findAllPaginatedJoin(
			new Long[]{pluginDriverId},
			PluginDriver_.ACL_MAPPINGS, DocTypeField.class,
			pageable.getLimit(), pageable.getSortBy().name(),
			pageable.getAfterId(), pageable.getBeforeId(),
			filter
		);
	}

	public Uni<Tuple2<PluginDriver, DocTypeField>> addDocTypeField(
		long pluginDriverId, long docTypeFieldId, UserField userField) {

		return sessionFactory.withTransaction((s) -> findById(s, pluginDriverId)
			.onItem()
			.ifNotNull()
			.transformToUni(pluginDriver ->
				docTypeFieldService.findById(s, docTypeFieldId)
					.onItem()
					.ifNotNull()
					.transformToUni(docTypeField -> s
						.fetch(pluginDriver.getAclMappings())
						.flatMap(aclMappings -> {

							AclMapping newAclMapping =
								AclMapping.of(
									PluginDriverDocTypeFieldKey.of(
										pluginDriverId, docTypeFieldId),
									pluginDriver, docTypeField, userField
								);

							if (aclMappings.add(newAclMapping)) {
								pluginDriver.setAclMappings(aclMappings);
								return persist(s, pluginDriver).map(ep -> Tuple2.of(
									ep,
									docTypeField
								));
							}
							else {
								return Uni.createFrom().nullItem();
							}

						})
					)
			)
		);
	}

	public Uni<Tuple2<PluginDriver, DocTypeField>> removeDocTypeField(
		long pluginDriverId,
		long docTypeFieldId) {

		return sessionFactory.withTransaction((s) -> findById(s, pluginDriverId)
			.onItem()
			.ifNotNull()
			.transformToUni(pluginDriver ->
				docTypeFieldService.findById(s, docTypeFieldId)
					.onItem()
					.ifNotNull()
					.transformToUni(docTypeField -> s
						.fetch(pluginDriver.getAclMappings())
						.flatMap(aclMappings -> {

							boolean removed = aclMappings.removeIf(
								epi -> epi.getKey().getDocTypeFieldId() == docTypeFieldId
									   && epi.getKey().getPluginDriverId() == pluginDriverId);

							if (removed) {
								return s.find(
										AclMapping.class,
										PluginDriverDocTypeFieldKey.of(pluginDriverId, docTypeFieldId)
									)
									.call(s::remove)
									.map(ep -> Tuple2.of(pluginDriver, docTypeField));
							}
							else {
								return Uni.createFrom().nullItem();
							}

						}))));
	}

	public Uni<Set<AclMapping>> getAclMappings(PluginDriver pluginDriver) {
		return sessionFactory.withTransaction(s -> s
			.merge(pluginDriver)
			.flatMap(merged -> s.fetch(merged.getAclMappings()))
		);
	}

	public Uni<AclMapping> setUserField(
		long pluginDriverId,
		long docTypeFieldId, UserField userField) {

		return sessionFactory.withTransaction(s -> {
			CriteriaBuilder criteria = sessionFactory.getCriteriaBuilder();

			CriteriaUpdate<AclMapping> query =
				criteria.createCriteriaUpdate(AclMapping.class);

			Root<AclMapping> from = query.from(AclMapping.class);

			query.where(criteria.and(
				criteria.equal(from
					.get(AclMapping_.key)
					.get(PluginDriverDocTypeFieldKey_.pluginDriverId), pluginDriverId),
				criteria.equal(from
					.get(AclMapping_.key)
					.get(PluginDriverDocTypeFieldKey_.docTypeFieldId), docTypeFieldId),
				criteria.notEqual(from.get(AclMapping_.userField), userField)
			));

			query.set(from.get(AclMapping_.userField), userField);

			return s.createQuery(query).executeUpdate().call(s::flush).flatMap(rowCount -> {
				if (rowCount == 0) {
					return Uni.createFrom().nullItem();
				}
				return s.find(
					AclMapping.class,
					PluginDriverDocTypeFieldKey.of(pluginDriverId, docTypeFieldId)
				);
			});
		});
	}


	public Uni<Response<PluginDriver>> createWithDocType(
		PluginWithDocTypeDTO dto) {

		return sessionFactory.withTransaction(
			(session, transaction) -> {
				var constraintViolations = validator.validate(dto);

				if (!constraintViolations.isEmpty()) {
					var fieldValidators = constraintViolations.stream()
						.map(constraintViolation -> FieldValidator.of(
							constraintViolation.getPropertyPath().toString(),
							constraintViolation.getMessage()))
						.collect(Collectors.toList());

					return Uni.createFrom().item(Response.of(null, fieldValidators));
				}

				return createWithDocType(session, dto)
					.flatMap(pluginDriver ->
						Uni.createFrom().item(Response.of(pluginDriver,null)));
			}
		);
	}

	public Uni<PluginDriver> createWithDocType(Mutiny.Session s, PluginWithDocTypeDTO dto) {

		var transientPluginDriver = mapper.create(dto);

		return super.create(s, transientPluginDriver)
			.flatMap(pluginDriver -> {
				var aclMappings = new LinkedHashSet<AclMapping>();

				for (PluginWithDocTypeDTO.DocTypeUserDTO docTypeUser
						: dto.getDocTypeUserDTOSet()) {

					var docTypeField =
						s.getReference(DocTypeField.class, docTypeUser.getDocTypeId());

					var aclMapping = new AclMapping();
					aclMapping.setPluginDriver(pluginDriver);
					aclMapping.setDocTypeField(docTypeField);
					aclMapping.setUserField(docTypeUser.getUserField());

					var key = PluginDriverDocTypeFieldKey.of(
						pluginDriver.getId(), docTypeUser.getDocTypeId());

					aclMapping.setKey(key);

					aclMappings.add(aclMapping);
				}

				pluginDriver.setAclMappings(aclMappings);

				return s
					.persist(pluginDriver)
					.flatMap(__ -> s.merge(pluginDriver));
			});
	}

	public Uni<Response<PluginDriver>> patchOrUpdateWithDocType(
		Long pluginId, PluginWithDocTypeDTO dto, boolean patch) {



		return sessionFactory.withTransaction(
			(session, transaction) -> {
				var constraintViolations = validator.validate(dto);

				if ( !constraintViolations.isEmpty() ) {
					var fieldValidators = constraintViolations.stream()
						.map(constraintViolation -> FieldValidator.of(
							constraintViolation.getPropertyPath().toString(),
							constraintViolation.getMessage()))
						.collect(Collectors.toList());

					return Uni.createFrom().item(Response.of(null, fieldValidators));
				}

				return patchOrUpdateWithDocType(session, pluginId, dto, patch)
					.flatMap(pluginDriver ->
						Uni.createFrom().item(Response.of(pluginDriver, null)));
			});
	}

	public Uni<PluginDriver> patchOrUpdateWithDocType(
		Mutiny.Session s, Long pluginId, PluginWithDocTypeDTO dto, boolean patch) {

		CriteriaBuilder cb = sessionFactory.getCriteriaBuilder();
		CriteriaDelete<AclMapping> deleteAclMapping =
			cb.createCriteriaDelete(AclMapping.class);
		Root<AclMapping> deleteFrom =
			deleteAclMapping.from(AclMapping.class);

		var docTypeUserDTOSet = dto.getDocTypeUserDTOSet();

		return findById(s, pluginId)
			.call(plugin -> Mutiny.fetch(plugin.getAclMappings()))
			.call(plugin -> {

				var pluginIdPath = deleteFrom.get("pluginDriver").get("id");
				var docTypeIdPath = deleteFrom.get("docTypeField").get("id");

				if ( docTypeUserDTOSet == null || docTypeUserDTOSet.isEmpty() ) {
					if ( patch ) {
						return Uni.createFrom().item(plugin);
					}
					else {
						deleteAclMapping.where(pluginIdPath.in(pluginId));

						//removes aclMapping old list
						return s.createQuery(deleteAclMapping).executeUpdate()
							.map(v -> plugin);
					}
				}
				else {
					//retrieves docType ids to keep
					var docTypeIdsToKeep = docTypeUserDTOSet.stream()
						.map(PluginWithDocTypeDTO.DocTypeUserDTO::getDocTypeId)
						.collect(Collectors.toSet());

					deleteAclMapping.where(
						cb.and(
							pluginIdPath.in(pluginId),
							cb.not(docTypeIdPath.in(docTypeUserDTOSet))
						));

					//removes aclMapping old list
					return s.createQuery(deleteAclMapping).executeUpdate()
						.map(v -> plugin);
				}
			})
			.call(s::flush)
			.call(Mutiny::fetch)
			.onItem().ifNotNull()
			.transformToUni(plugin -> {

				PluginDriver newPluginDriver;
				var newHashSet = new HashSet<AclMapping>();

				if ( patch ) {
					newPluginDriver = mapper.patch(plugin, dto);
				}
				else {
					newPluginDriver = mapper.patch(plugin, dto);
					newPluginDriver.setAclMappings(newHashSet);
				}

				//set new aclMapping Set
				if ( docTypeUserDTOSet != null ) {
					newPluginDriver.setAclMappings(newHashSet);

					docTypeUserDTOSet.forEach(docTypeUserDTO -> {
						var docTypeId = docTypeUserDTO.getDocTypeId();
						var key =
							PluginDriverDocTypeFieldKey.of(pluginId, docTypeId);
						var docTypeReference =
							s.getReference(DocTypeField.class, docTypeId);

						var aclMapping = new AclMapping();
						aclMapping.setPluginDriver(plugin);
						aclMapping.setDocTypeField(docTypeReference);
						aclMapping.setKey(key);
						aclMapping.setUserField(docTypeUserDTO.getUserField());

						newPluginDriver.getAclMappings().add(aclMapping);
					});
				}

				return s.merge(newPluginDriver)
					.map(v -> newPluginDriver)
					.call(s::flush);
			});
	}
}
