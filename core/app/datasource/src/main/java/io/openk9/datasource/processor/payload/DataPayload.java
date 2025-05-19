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

package io.openk9.datasource.processor.payload;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.openk9.common.util.ingestion.PayloadType;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@RegisterForReflection
public class DataPayload {
	private String ingestionId;
	private long datasourceId;
	private String contentId;
	private long parsingDate;
	private String rawContent;
	private String tenantId;
	private String[] documentTypes;
	private ResourcesPayload resources;
	private Map<String, List<String>> acl;
	@Setter(AccessLevel.NONE)
	@JsonIgnore
	@Builder.Default
	private Map<String, Object> rest = new HashMap<>();
	private String indexName;
	private String scheduleId;
	private String oldIndexName;
	private PayloadType type;

	public static DataPayload copy(DataPayload dataPayload) {
		return dataPayload.toBuilder().build();
	}

	public DataPayload rest(Map<String, Object> rest) {
		return this.toBuilder()
			.rest(rest)
			.build();
	}

	@JsonAnySetter
	public void addRest(String key, Object value) {

		if (value == null) {
			return;
		}

		if (value instanceof Collection) {
			if (((Collection) value).isEmpty()) {
				return;
			}
		}
		else if (value instanceof Map) {
			if (((Map) value).isEmpty()) {
				return;
			}
		}

		rest.put(key, value);

	}

	@JsonAnyGetter
	public Map<String, Object> getRest() {
		return rest;
	}

}
