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

package io.openk9.datasource.model.dto;

import com.cronutils.model.CronType;
import com.cronutils.validation.Cron;
import io.openk9.datasource.model.dto.util.K9EntityDTO;
import io.openk9.datasource.validation.json.Json;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.eclipse.microprofile.graphql.Description;

@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DatasourceDTO extends K9EntityDTO {

	@NotNull
	@Cron(type = CronType.QUARTZ)
	@Description("Chron quartz expression to define scheduling of datasource")
	private String scheduling;

	@NotNull
	@Description("If true datasource is scheduled based on defined scheduling expression")
	@Builder.Default
	private Boolean schedulable = false;

	@Json
	@Description("Json configuration with custom fields for datasource")
	private String jsonConfig;

	@Deprecated
	@Description("Reindex on datasource every {reindexRate} times, never if 0")
	@NotNull
	private int reindexRate;

}
