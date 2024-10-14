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

package io.openk9.entity.manager.web;

import io.openk9.entity.manager.dto.EntityManagerDataPayload;
import io.openk9.entity.manager.processor.EntityManagerConsumer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Path("/consume")
public class EntityManagerEndpoint {

	@PostConstruct
	public void init() {
		_executorService = Executors.newFixedThreadPool(1);
	}

	@PreDestroy
	public void destroy(){
		_executorService.shutdown();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void consume(EntityManagerDataPayload payload) {

		_executorService.execute(() -> {
			entityManagerConsumer.consume(payload);
		});

	}

	@Inject
	EntityManagerConsumer entityManagerConsumer;

	private ExecutorService _executorService;

}