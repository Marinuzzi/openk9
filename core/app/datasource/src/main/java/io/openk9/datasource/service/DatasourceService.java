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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;

import io.openk9.common.graphql.util.relay.Connection;
import io.openk9.common.util.FieldValidator;
import io.openk9.common.util.Response;
import io.openk9.common.util.SortBy;
import io.openk9.datasource.graphql.dto.CreateDatasourceDTO;
import io.openk9.datasource.mapper.DatasourceMapper;
import io.openk9.datasource.model.DataIndex;
import io.openk9.datasource.model.Datasource;
import io.openk9.datasource.model.Datasource_;
import io.openk9.datasource.model.EnrichPipeline;
import io.openk9.datasource.model.PluginDriver;
import io.openk9.datasource.model.Scheduler;
import io.openk9.datasource.model.Scheduler_;
import io.openk9.datasource.model.dto.DatasourceDTO;
import io.openk9.datasource.model.dto.UpdateDatasourceDTO;
import io.openk9.datasource.model.util.K9Entity;
import io.openk9.datasource.service.exception.K9Error;
import io.openk9.datasource.service.util.BaseK9EntityService;
import io.openk9.datasource.service.util.Tuple2;

import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DatasourceService extends BaseK9EntityService<Datasource, DatasourceDTO> {

	private static final String UPDATE_DATASOURCE = "DatasourceService#updateDatasource";
	private static final Logger log = Logger.getLogger(DatasourceService.class);

	@Inject
	DataIndexService dataIndexService;
	@Inject
	EnrichPipelineService enrichPipelineService;
	@Inject
	PluginDriverService pluginDriverService;
	@Inject
	SchedulerService schedulerService;

	DatasourceService(DatasourceMapper mapper) {
		this.mapper = mapper;
	}

	public static CompletableFuture<Void> updateDatasource(
		String tenantId,
		long datasourceId,
		OffsetDateTime lastIngestionDate,
		Long newDataIndexId) {

		var eventBus = CDI.current().select(EventBus.class).get();

		return eventBus.request(UPDATE_DATASOURCE, new UpdateDatasourceRequest(
				tenantId, datasourceId, lastIngestionDate, newDataIndexId))
			.map(message -> (Void) message.body())
			.subscribeAsCompletionStage();
	}

	public Uni<Tuple2<Datasource, PluginDriver>> createDatasourceAndAddPluginDriver(
		DatasourceDTO datasourceDTO, long pluginDriverId) {

		return sessionFactory.withTransaction(s -> pluginDriverService
			.findById(s, pluginDriverId)
			.onItem()
			.ifNotNull()
			.transformToUni(pluginDriver -> {
				Datasource dataSource = mapper.create(datasourceDTO);
				dataSource.setPluginDriver(pluginDriver);
				return persist(s, dataSource).map(d -> Tuple2.of(d, pluginDriver));
			}));

	}

	public Uni<Response<Datasource>> createDatasourceConnection(
		CreateDatasourceDTO datasourceConnection) {

		return Uni.createFrom().item(() -> {
				checkExclusiveFields(datasourceConnection);

				var constraintViolations = validator.validate(datasourceConnection);

				if (!constraintViolations.isEmpty()) {
					throw new ConstraintViolationException(constraintViolations);
				}

				return datasourceConnection;
			})
			.flatMap(createDatasourceDTO -> sessionFactory.withTransaction((session, transaction) ->
				getOrCreatePluginDriver(session, datasourceConnection)
					.flatMap(pluginDriver -> getOrCreateEnrichPipeline(
							session,
							datasourceConnection
						).flatMap(enrichPipeline -> {
							var datasource = mapper.create(datasourceConnection);

							datasource.setPluginDriver(pluginDriver);
							datasource.setEnrichPipeline(enrichPipeline);

							return create(session, datasource);
						}).flatMap(datasource -> dataIndexService
							.createDataIndex(
								session,
								datasource,
								createDatasourceDTO.getDataIndex()
							)
							.invoke(datasource::setDataIndex)
							.flatMap(__ -> persist(session, datasource))
						)
					)
			))
			.onItemOrFailure()
			.transformToUni((datasource, throwable) -> {
				if (throwable != null) {
					if (throwable instanceof ConstraintViolationException constraintViolations) {
						var fieldValidators =
							constraintViolations.getConstraintViolations().stream()
								.map(constraintViolation -> FieldValidator.of(constraintViolation
									.getPropertyPath()
									.toString(), constraintViolation.getMessage()))
								.collect(Collectors.toList());
						return Uni.createFrom().item(Response.of(null, fieldValidators));
					}
					if (throwable instanceof ValidationException validationException) {
						return Uni.createFrom().item(Response.of(
							null,
							List.of(FieldValidator.of("error", validationException.getMessage()))
						));
					}
					return Uni.createFrom().failure(new K9Error(throwable));
				}
				else {
					return Uni.createFrom().item(Response.of(datasource, null));
				}
			});
	}

	public Uni<Datasource> deleteById(long datasourceId) {

		var cb = sessionFactory.getCriteriaBuilder();

		return sessionFactory.withTransaction(
			(session, transaction) ->
				findById(session, datasourceId)
					.call(datasource -> session.fetch(datasource.getDataIndex()))
					.call(datasource -> session.fetch(datasource.getPluginDriver()))
					.call(datasource -> session.fetch(datasource.getEnrichPipeline()))
					.call(datasource -> session.fetch(datasource.getBuckets()))
					.flatMap(datasource -> {
						// dereferences entities on datasource
						datasource.setDataIndex(null);
						datasource.setPluginDriver(null);
						datasource.setEnrichPipeline(null);
						datasource.setBuckets(null);

						return merge(session, datasource)
							.flatMap(__ -> {
								// deletes all schedulers
								var deleteScheduler =
									cb.createCriteriaDelete(Scheduler.class);
								var deleteFromScheduler = deleteScheduler.from(Scheduler.class);

								deleteScheduler.where(cb.equal(
										deleteFromScheduler.get(Scheduler_.datasource),
										datasource
									)
								);

								return session.createQuery(deleteScheduler).executeUpdate();
							})
							// deletes dataIndices
							.call(__ -> getDataIndexes(datasourceId)
								.flatMap(dataIndices -> {
									var dataIndexIds = dataIndices.stream()
										.map(K9Entity::getId)
										.collect(Collectors.toSet());

									return dataIndexService.deleteAllByIds(dataIndexIds);
								})
							)
							.flatMap(unused -> super.deleteById(session, datasourceId))
							.map(integer -> datasource);
					})
		);

	}

	public Uni<Datasource> deleteById(long datasourceId, String datasourceName) {

		return findById(datasourceId)
			.flatMap(datasource -> {
				if (!datasource.getName().equals(datasourceName)) {
					throw new ValidationException(
						"datasourceName is not the same");
				}
				return deleteById(datasourceId);
			});
	}

	public Uni<Datasource> findByIdWithPluginDriver(long datasourceId) {
		return sessionFactory.withTransaction((s) ->
			findByIdWithPluginDriver(s, datasourceId));
	}

	public Uni<Datasource> findByIdWithPluginDriver(Mutiny.Session session, long datasourceId) {
		return session.createQuery("""
				from Datasource d
				left join fetch d.pluginDriver
				where d.id = :id
				""", Datasource.class)
			.setParameter("id", datasourceId)
			.getSingleResult();
	}

	@Override
	public Class<Datasource> getEntityClass() {
		return Datasource.class;
	}

	public Uni<DataIndex> getDataIndex(Datasource datasource) {
		return sessionFactory.withTransaction(s -> s.fetch(datasource.getDataIndex()));
	}

	public Uni<DataIndex> getDataIndex(long datasourceId) {
		return sessionFactory.withTransaction(s -> findById(
			s,
			datasourceId
		).flatMap(datasource -> s.fetch(datasource.getDataIndex())));
	}

	public Uni<List<DataIndex>> getDataIndexes(long datasourceId) {
		return sessionFactory.withTransaction(s -> findById(s, datasourceId)
			.flatMap(datasource -> s.fetch(datasource.getDataIndexes()))
			.map(ArrayList::new));
	}

	public Uni<List<DataIndex>> getDataIndexOrphans(long datasourceId) {
		return sessionFactory.withTransaction((s) -> s.createQuery(
			"select di "
			+ "from DataIndex di "
			+ "inner join di.datasource d on di.datasource = d and d.dataIndex <> di "
			+ "where d.id = :id",
			DataIndex.class
		).setParameter("id", datasourceId).getResultList());
	}

	public Uni<Connection<DataIndex>> getDataIndexConnection(
		Long id,
		String after,
		String before,
		Integer first,
		Integer last,
		String searchText,
		Set<SortBy> sortByList,
		boolean notEqual) {

		return findJoinConnection(
			id,
			Datasource_.DATA_INDEXES,
			DataIndex.class,
			dataIndexService.getSearchFields(),
			after,
			before,
			first,
			last,
			searchText,
			sortByList,
			notEqual
		);
	}

	public Uni<EnrichPipeline> getEnrichPipeline(Datasource datasource) {
		return sessionFactory.withTransaction(s -> s.fetch(datasource.getEnrichPipeline()));
	}

	public Uni<EnrichPipeline> getEnrichPipeline(long datasourceId) {
		return sessionFactory.withTransaction(s -> findById(
			s,
			datasourceId
		).flatMap(datasource -> s.fetch(datasource.getEnrichPipeline())));
	}

	public Uni<Set<DataIndex>> getDataIndexes(Datasource datasource) {
		return sessionFactory.withTransaction(s -> s.fetch(datasource.getDataIndexes()));
	}

	public Uni<PluginDriver> getPluginDriver(long datasourceId) {
		return sessionFactory.withTransaction(s -> findById(
			s,
			datasourceId
		).flatMap(datasource -> s.fetch(datasource.getPluginDriver())));
	}

	public Uni<Connection<Scheduler>> getSchedulerConnection(
		Long id,
		String after,
		String before,
		Integer first,
		Integer last,
		String searchText,
		Set<SortBy> sortByList,
		boolean notEqual) {

		return findJoinConnection(
			id,
			Datasource_.SCHEDULERS,
			Scheduler.class,
			schedulerService.getSearchFields(),
			after,
			before,
			first,
			last,
			searchText,
			sortByList,
			notEqual
		);
	}

	public Uni<Tuple2<Datasource, DataIndex>> setDataIndex(
		Mutiny.Session session, long datasourceId, long dataIndexId) {

		return findById(session, datasourceId)
			.onItem()
			.ifNotNull()
			.transformToUni(datasource -> dataIndexService
				.findById(session, dataIndexId)
				.onItem()
				.ifNotNull()
				.transformToUni(dataIndex -> {
					datasource.setDataIndex(dataIndex);
					return persist(session, datasource)
						.map(d -> Tuple2.of(d, dataIndex));
				}));
	}

	public Uni<Tuple2<Datasource, DataIndex>> setDataIndex(long datasourceId, long dataIndexId) {
		return sessionFactory.withTransaction(s ->
			setDataIndex(s, datasourceId, dataIndexId));
	}

	public Uni<Tuple2<Datasource, EnrichPipeline>> setEnrichPipeline(
		long datasourceId, long enrichPipelineId) {
		return sessionFactory.withTransaction(s -> findById(s, datasourceId)
			.onItem()
			.ifNotNull()
			.transformToUni(datasource -> enrichPipelineService
				.findById(s, enrichPipelineId)
				.onItem()
				.ifNotNull()
				.transformToUni(enrichPipeline -> {
					datasource.setEnrichPipeline(enrichPipeline);
					return persist(s, datasource).map(d -> Tuple2.of(d, enrichPipeline));
				})));
	}

	public Uni<Tuple2<Datasource, PluginDriver>> setPluginDriver(
		long datasourceId, long pluginDriverId) {

		return sessionFactory.withTransaction(s -> findById(s, datasourceId)
			.onItem()
			.ifNotNull()
			.transformToUni(datasource -> pluginDriverService
				.findById(s, pluginDriverId)
				.flatMap(pluginDriver -> {
					datasource.setPluginDriver(pluginDriver);
					return persist(s, datasource).map(d -> Tuple2.of(d, pluginDriver));
				})));
	}

	@Override
	public String[] getSearchFields() {
		return new String[]{Datasource_.NAME, Datasource_.DESCRIPTION};
	}

	public Uni<Datasource> unsetEnrichPipeline(long datasourceId) {
		return sessionFactory.withTransaction(s -> findById(s, datasourceId)
			.onItem()
			.ifNotNull()
			.transformToUni(datasource -> {
				datasource.setEnrichPipeline(null);
				return persist(s, datasource);
			}));
	}

	public Uni<Datasource> unsetDataIndex(long datasourceId) {
		return sessionFactory.withTransaction(s -> findById(s, datasourceId)
			.onItem()
			.ifNotNull()
			.transformToUni(datasource -> {
				datasource.setDataIndex(null);
				return persist(s, datasource);
			}));
	}

	public Uni<Datasource> unsetPluginDriver(long datasourceId) {
		return sessionFactory.withTransaction(s -> findById(s, datasourceId)
			.onItem()
			.ifNotNull()
			.transformToUni(datasource -> {
				datasource.setPluginDriver(null);
				return persist(s, datasource);
			}));
	}

	public Uni<Response<Datasource>> updateDatasourceConnection(
		UpdateDatasourceDTO updateConnectionDTO) {

		return Uni.createFrom()
			.item(() -> {
				var datasourceId = updateConnectionDTO.getDatasourceId();

				if (datasourceId == 0L) {
					throw new ValidationException("Request must defines datasourceId");
				}

				var dataIndexId = updateConnectionDTO.getDataIndexId();

				if (dataIndexId == 0L) {
					throw new ValidationException("Request must defines dataIndexId");
				}

				var constraintViolations = validator.validate(updateConnectionDTO);

				if (!constraintViolations.isEmpty()) {
					throw new ConstraintViolationException(constraintViolations);
				}

				return updateConnectionDTO;
			})
			.flatMap(__ -> sessionFactory.withTransaction((s, t) ->
					updateDatasourceConnection(s, updateConnectionDTO))
				.onItemOrFailure()
				.transformToUni((datasource, throwable) -> {
					if (throwable != null) {
						if (throwable instanceof ConstraintViolationException constraintViolations) {
							var fieldValidators =
								constraintViolations.getConstraintViolations().stream()
									.map(constraintViolation -> FieldValidator.of(
										constraintViolation
											.getPropertyPath()
											.toString(),
										constraintViolation.getMessage()
									))
									.collect(Collectors.toList());
							return Uni.createFrom().item(Response.of(null, fieldValidators));
						}
						if (throwable instanceof ValidationException validationException) {
							return Uni.createFrom().item(Response.of(
								null,
								List.of(FieldValidator.of(
									"error",
									validationException.getMessage()
								))
							));
						}
						return Uni.createFrom().failure(new K9Error(throwable));
					}
					else {
						return Uni.createFrom().item(Response.of(datasource, null));
					}
				})
			);

	}

	public Uni<Datasource> updateDatasourceConnection(
		Mutiny.Session s, UpdateDatasourceDTO updateConnectionDTO) {

		return findByIdWithPluginDriver(s, updateConnectionDTO.getDatasourceId())
			.flatMap(datasource -> updateOrCreateEnrichPipeline(s, updateConnectionDTO)
				.invoke(datasource::setEnrichPipeline)
				.map(enrichPipeline -> s.getReference(
					DataIndex.class, updateConnectionDTO.getDataIndexId()))
				.invoke(datasource::setDataIndex)
				.map(__ -> mapper.update(datasource, updateConnectionDTO))
				.chain(newState -> merge(s, newState)));
	}

	@ConsumeEvent(UPDATE_DATASOURCE)
	Uni<Void> _updateDatasource(UpdateDatasourceRequest request) {

		var tenantId = request.tenantId();
		var datasourceId = request.datasourceId();
		var lastIngestionDate = request.lastIngestionDate();
		var newDataIndexId = request.newDataIndexId();

		return sessionFactory.withTransaction(
			tenantId, (s, t) -> s.find(Datasource.class, datasourceId)
				.flatMap(datasource -> {
					datasource.setLastIngestionDate(lastIngestionDate);

					if (newDataIndexId != null) {
						var newDataIndex = s.getReference(DataIndex.class, newDataIndexId);

						log.infof(
							"replacing dataindex %s for datasource %s on tenant %s",
							newDataIndexId, datasourceId, tenantId
						);

						datasource.setDataIndex(newDataIndex);
					}

					return s.persist(datasource);
				})
		);
	}

	private void checkExclusiveFields(CreateDatasourceDTO createDatasourceDTO)
	throws ValidationException {

		var pluginDriver = createDatasourceDTO.getPluginDriver();
		var pluginDriverId = createDatasourceDTO.getPluginDriverId();


		if (pluginDriver == null && pluginDriverId == null) {
			throw new ValidationException(
				"Request must defines one of pluginDriverId or pluginDriver");
		}

		if (pluginDriver != null && pluginDriverId != null) {
			throw new ValidationException(
				"Ambiguous Request: defines pluginDriver or pluginDriverId, exclusively");
		}

		var pipeline = createDatasourceDTO.getPipeline();
		var pipelineId = createDatasourceDTO.getPipelineId();

		if (pipeline != null && pipelineId != null) {
			throw new ValidationException(
				"Ambiguous Request: defines pipeline or pipelineId, exclusively");
		}

	}

	private Uni<EnrichPipeline> getOrCreateEnrichPipeline(
		Mutiny.Session session,
		CreateDatasourceDTO createDatasourceDTO) {

		var pipelineDto = createDatasourceDTO.getPipeline();
		var pipelineId = createDatasourceDTO.getPipelineId();

		if (pipelineDto != null) {
			return enrichPipelineService.createWithItems(session, pipelineDto);
		}
		else if (pipelineId != null) {
			return enrichPipelineService.findById(session, pipelineId);
		}
		else {
			return Uni.createFrom().nullItem();
		}
	}

	private Uni<PluginDriver> getOrCreatePluginDriver(
		Mutiny.Session session,
		CreateDatasourceDTO createDatasourceDTO) {

		var pluginDriverDto = createDatasourceDTO.getPluginDriver();

		if (pluginDriverDto != null) {
			return pluginDriverService.create(session, pluginDriverDto);
		}
		else {
			return pluginDriverService.findById(
				session,
				createDatasourceDTO.getPluginDriverId()
			);
		}
	}

	private Uni<EnrichPipeline> updateOrCreateEnrichPipeline(
		Mutiny.Session session,
		UpdateDatasourceDTO updateConnectionDTO) {

		var pipelineId = updateConnectionDTO.getPipelineId();
		var pipelineDto = updateConnectionDTO.getPipeline();

		if (pipelineId != null && pipelineDto != null) {
			return enrichPipelineService.patchOrUpdateWithItems(
				session, pipelineId, pipelineDto, false);
		}
		else if (pipelineId == null && pipelineDto != null) {
			return enrichPipelineService.createWithItems(session, pipelineDto);
		}
		else if (pipelineId != null) {
			return enrichPipelineService.findById(session, pipelineId);
		}
		else {
			return Uni.createFrom().nullItem();
		}

	}

	public record UpdateDatasourceRequest(
		String tenantId, long datasourceId, OffsetDateTime lastIngestionDate, Long newDataIndexId
	) {}
}
