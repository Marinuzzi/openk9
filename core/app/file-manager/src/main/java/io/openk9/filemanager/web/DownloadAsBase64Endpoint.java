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

package io.openk9.filemanager.web;

import io.openk9.filemanager.service.DownloadService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Path("/v1/download/base64")
public class DownloadAsBase64Endpoint {

	@GET
	@Path("/{resourceId}/{schemaName}")
	@Produces(MediaType.TEXT_PLAIN)
	public String downloadAsBase64(@PathParam("resourceId") String resourceId,
								   @PathParam("schemaName") String schemaName) {

		InputStream inputStream = downloadService.
			downloadObject(resourceId, schemaName);

		byte[] sourceBytes;

		try {
			sourceBytes = IOUtils.toByteArray(inputStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return Base64.getEncoder().encodeToString(sourceBytes);

	}

	@Inject
	DownloadService downloadService;

}