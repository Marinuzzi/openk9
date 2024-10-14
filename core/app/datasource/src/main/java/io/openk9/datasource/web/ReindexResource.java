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

package io.openk9.datasource.web;

import io.openk9.datasource.listener.SchedulerInitializer;
import io.openk9.datasource.service.SchedulerService;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import lombok.Data;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import java.util.List;

@CircuitBreaker
@Path("/v1/index")
@ApplicationScoped
@ActivateRequestContext
@RolesAllowed("k9-admin")
public class ReindexResource {

	@POST
	@Path("/reindex")
	public Uni<List<SchedulerService.DatasourceJobStatus>> reindex(ReindexRequestDto dto) {
		List<Long> datasourceIds = dto.datasourceIds;
		String tenantId = routingContext.get("_tenantId");
		return schedulerService
			.getStatusByDatasources(datasourceIds)
			.call(() -> schedulerInitializer
				.get()
				.triggerJobs(tenantId, dto.getDatasourceIds(), true)
			);
	}

	@Inject
	Instance<SchedulerInitializer> schedulerInitializer;

	@Inject
	RoutingContext routingContext;

	@Inject
	SchedulerService schedulerService;

	@Data
	public static class ReindexRequestDto {
		private List<Long> datasourceIds;
	}

}
