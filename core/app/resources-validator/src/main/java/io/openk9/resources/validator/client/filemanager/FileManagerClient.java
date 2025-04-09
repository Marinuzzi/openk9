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

package io.openk9.resources.validator.client.filemanager;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.InputStream;

@Path("/api/file-manager/v1")
@RegisterRestClient(configKey = "file-manager")
public interface FileManagerClient {

	@GET
	@Path("/download/{resourceId}/{schemaName}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	InputStream download(@PathParam("resourceId") String resourceId,
						 @PathParam("schemaName") String schemaName);

	@POST
	@Path("/delete/{resourceId}/{schemaName}")
	void delete(
		@jakarta.ws.rs.PathParam("resourceId") String resourceId,
				@PathParam("schemaName") String schemaName);

}
