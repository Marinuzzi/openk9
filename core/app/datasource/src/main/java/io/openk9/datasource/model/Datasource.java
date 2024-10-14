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

package io.openk9.datasource.model;

import com.cronutils.model.CronType;
import com.cronutils.validation.Cron;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.openk9.datasource.listener.K9EntityListener;
import io.openk9.datasource.model.util.K9Entity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.microprofile.graphql.Description;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "datasource")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EntityListeners(K9EntityListener.class)
public class Datasource extends K9Entity {

	@Column(name = "name", nullable = false, unique = true)
	private String name;

	@Column(name = "description", length = 4096)
	private String description;

	@Description("Chron quartz expression to define scheduling of datasource")
	@Column(name = "scheduling", nullable = false)
	@Cron(type = CronType.QUARTZ)
	private String scheduling;

	@Description("Last ingestion date of data for current datasource")
	@Column(name = "last_ingestion_date")
	private OffsetDateTime lastIngestionDate;

	@Description("If true set datasource as schedulable")
	@Column(name = "schedulable", nullable = false)
	private Boolean schedulable = false;

	@ToString.Exclude
	@OneToOne(
		fetch = jakarta.persistence.FetchType.LAZY, cascade = jakarta.persistence.CascadeType.ALL
	)
	@JoinColumn(name = "data_index_id", referencedColumnName = "id")
	@JsonIgnore
	private DataIndex dataIndex;

	@ToString.Exclude
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "datasource")
	@JsonIgnore
	private Set<DataIndex> dataIndexes;

	@ToString.Exclude
	@ManyToOne(cascade = {
		jakarta.persistence.CascadeType.PERSIST,
		jakarta.persistence.CascadeType.MERGE,
		jakarta.persistence.CascadeType.REFRESH,
		jakarta.persistence.CascadeType.DETACH
	}
	)
	@JoinColumn(name = "enrich_pipeline_id")
	@JsonIgnore
	private EnrichPipeline enrichPipeline;

	@ToString.Exclude
	@ManyToOne(
		fetch = FetchType.LAZY,
		cascade = {
			jakarta.persistence.CascadeType.PERSIST,
			jakarta.persistence.CascadeType.MERGE,
			jakarta.persistence.CascadeType.REFRESH,
			jakarta.persistence.CascadeType.DETACH
		}
	)
	@JoinColumn(name = "plugin_driver_id")
	@JsonIgnore
	private PluginDriver pluginDriver;

	@ManyToMany(cascade = {
		jakarta.persistence.CascadeType.PERSIST,
		jakarta.persistence.CascadeType.MERGE,
		jakarta.persistence.CascadeType.DETACH,
		jakarta.persistence.CascadeType.REFRESH
	}
	)
	@JoinTable(name = "datasource_buckets",
		joinColumns = @JoinColumn(name = "datasource_id", referencedColumnName = "id"),
		inverseJoinColumns = @JoinColumn(name = "buckets_id", referencedColumnName = "id"))
	@ToString.Exclude
	@JsonIgnore
	private Set<Bucket> buckets = new LinkedHashSet<>();

	@OneToMany(mappedBy = "datasource")
	@ToString.Exclude
	@JsonIgnore
	private Set<Scheduler> schedulers = new LinkedHashSet<>();

	@JdbcTypeCode(Types.LONGVARCHAR)
	@Column(name = "json_config")
	private String jsonConfig;

	@Description("Reindex on datasource every {reindexRate} times, never if 0")
	@Column(name = "reindex_rate")
	private int reindexRate = 0;

}