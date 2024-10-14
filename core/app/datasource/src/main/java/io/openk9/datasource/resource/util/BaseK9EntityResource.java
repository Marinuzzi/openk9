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

package io.openk9.datasource.resource.util;

import io.openk9.datasource.model.dto.util.K9EntityDTO;
import io.openk9.datasource.model.util.K9Entity;
import io.openk9.datasource.service.util.BaseK9EntityService;
import io.openk9.datasource.service.util.K9EntityEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.jboss.resteasy.reactive.RestStreamElementType;

@CircuitBreaker
@RolesAllowed("k9-admin")
public abstract class BaseK9EntityResource<
	SERVICE extends BaseK9EntityService<ENTITY, DTO>,
	ENTITY extends K9Entity,
	DTO extends K9EntityDTO> {

	protected BaseK9EntityResource(SERVICE service) {
		this.service = service;
	}

	@GET
	public Uni<Page<ENTITY>> findAll(
		@BeanParam Pageable pageable,
		@QueryParam("searchText") String searchText) {
		return this.service.findAllPaginated(pageable, searchText);
	}

	@GET
	@Path("/count")
	public Uni<Long> count() {
		return this.service.count();
	}

	@GET
	@Path("/{id}")
	public Uni<ENTITY> findById(@PathParam("id") long id) {
		return this.service.findById(id);
	}

	@PATCH
	@Path("/{id}")
	public Uni<ENTITY> patch(@PathParam("id") long id, DTO dto) {
		return this.service.patch(id, dto);
	}

	@PUT
	@Path("/{id}")
	public Uni<ENTITY> update(@PathParam("id") long id, DTO dto) {
		return this.service.update(id, dto);
	}

	@POST
	public Uni<ENTITY> persist(DTO entity) {
		return this.service.create(entity);
	}

	@DELETE
	@Path("/{id}")
	public Uni<ENTITY> deleteById(@PathParam("id") long entityId) {
		return this.service.deleteById(entityId);
	}

	@GET
	@Path("/stream")
	@Produces(MediaType.SERVER_SENT_EVENTS)
	@RestStreamElementType(MediaType.APPLICATION_JSON)
	public Multi<K9EntityEvent<ENTITY>> getProcessor() {
		return this.service.getProcessor();
	}

	protected final SERVICE service;

}
