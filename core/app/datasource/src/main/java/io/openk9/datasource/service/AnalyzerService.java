package io.openk9.datasource.service;

import io.openk9.common.graphql.util.relay.Connection;
import io.openk9.common.util.SortBy;
import io.openk9.datasource.mapper.AnalyzerMapper;
import io.openk9.datasource.model.Analyzer;
import io.openk9.datasource.model.Analyzer_;
import io.openk9.datasource.model.CharFilter;
import io.openk9.datasource.model.TokenFilter;
import io.openk9.datasource.model.Tokenizer;
import io.openk9.datasource.model.dto.AnalyzerDTO;
import io.openk9.datasource.service.util.BaseK9EntityService;
import io.openk9.datasource.service.util.Tuple2;
import io.smallrye.mutiny.Uni;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

;

@ApplicationScoped
public class AnalyzerService extends BaseK9EntityService<Analyzer, AnalyzerDTO> {
	AnalyzerService(AnalyzerMapper mapper) {
		this.mapper = mapper;
	}

	public Uni<List<Analyzer>> findUnboundAnalyzersByTokenFilter(long tokenFilterId) {
		return sessionFactory.withTransaction(s -> {
			String queryString = "SELECT analyzer.* from analyzer " +
				"WHERE analyzer.id not in (" +
				"SELECT analyzer_token_filter.analyzer FROM analyzer_token_filter " +
				"WHERE analyzer_token_filter.token_filter = (:tokenFilterId))";

			return s.createNativeQuery(queryString, Analyzer.class)
				.setParameter("tokenFilterId", tokenFilterId)
				.getResultList();
		});
	}

	public Uni<List<Analyzer>> findUnboundAnalyzersByCharFilter(long charFilterId) {
		return sessionFactory.withTransaction(s -> {
			String queryString = "SELECT analyzer.* from analyzer " +
				"WHERE analyzer.id not in (" +
				"SELECT analyzer_char_filter.analyzer FROM analyzer_char_filter " +
				"WHERE analyzer_char_filter.char_filter = (:charFilterId))";

			return s.createNativeQuery(queryString, Analyzer.class)
				.setParameter("charFilterId", charFilterId)
				.getResultList();
		});
	}

	@Override
	public Class<Analyzer> getEntityClass() {return Analyzer.class;} ;


	@Override
	public String[] getSearchFields() {
		return new String[] {Analyzer_.NAME, Analyzer_.DESCRIPTION, Analyzer_.TYPE};
	}

	public Uni<Connection<TokenFilter>> getTokenFilters(
		Long id, String after, String before, Integer first, Integer last,
		String searchText, Set<SortBy> sortByList, boolean notEqual) {

		return findJoinConnection(
			id, Analyzer_.TOKEN_FILTERS, TokenFilter.class,
			_tokenFilterService.getSearchFields(), after, before, first,
			last, searchText, sortByList, notEqual);
	}

	public Uni<Connection<CharFilter>> getCharFilters(
		Long id, String after, String before, Integer first, Integer last,
		String searchText, Set<SortBy> sortByList, boolean notEqual) {

		return findJoinConnection(
			id, Analyzer_.CHAR_FILTERS, CharFilter.class,
			_charFilterService.getSearchFields(), after, before, first,
			last, searchText, sortByList, notEqual);
	}

	public Uni<Tokenizer> getTokenizer(long analyzerId) {
		return sessionFactory.withTransaction(s -> findById(s, analyzerId)
				.flatMap(analyzer -> s.fetch(analyzer.getTokenizer())));
	}

	public Uni<Tuple2<Analyzer, TokenFilter>> addTokenFilterToAnalyzer(
		long id, long tokenFilterId) {

		return sessionFactory.withTransaction((s, tr) -> findById(s, id)
			.onItem()
			.ifNotNull()
			.transformToUni(analyzer -> _tokenFilterService.findById(s, tokenFilterId)
				.onItem()
				.ifNotNull()
				.transformToUni(tokenFilter ->
					s.fetch(analyzer.getTokenFilters())
						.onItem()
						.ifNotNull()
						.transformToUni(tokenFilters -> {

							if (tokenFilters.add(tokenFilter)) {

								analyzer.setTokenFilters(tokenFilters);

								return persist(s, analyzer)
									.map(newSC -> Tuple2.of(newSC, tokenFilter));
							}

							return Uni.createFrom().nullItem();

						})
				)
			));
	}

	public Uni<Tuple2<Analyzer, TokenFilter>> removeTokenFilterToAnalyzer(
		long id, long tokenFilterId) {

		return sessionFactory.withTransaction((s, tr) -> findById(s, id)
			.onItem()
			.ifNotNull()
			.transformToUni(analyzer -> s.fetch(analyzer.getTokenFilters())
				.onItem()
				.ifNotNull()
				.transformToUni(tokenFilters -> {

					if (analyzer.removeTokenFilter(tokenFilters, tokenFilterId)) {

						return persist(s, analyzer)
							.map(newSC -> Tuple2.of(newSC, null));
					}

					return Uni.createFrom().nullItem();

				})));
	}

	public Uni<Analyzer> removeTokenFilterListFromAnalyzer(
		long analyzerId) {
		return sessionFactory.withTransaction((s, tr) -> findById(s, analyzerId)
			.onItem()
			.ifNotNull()
			.transformToUni(analyzer -> s.fetch(analyzer.getTokenFilters())
				.onItem()
				.ifNotNull()
				.transformToUni(tokenFilters -> {

					if(!tokenFilters.isEmpty()){
						tokenFilters.clear();
						return persist(s, analyzer);
					};

					return Uni.createFrom().nullItem();

				})));
	}

	public Uni<Analyzer> removeCharFilterListFromAnalyzer(long analyzerId) {
		return sessionFactory.withTransaction((s, tr) -> findById(s, analyzerId)
			.onItem()
			.ifNotNull()
			.transformToUni(analyzer -> s.fetch(analyzer.getCharFilters())
				.onItem()
				.ifNotNull()
				.transformToUni(charFilters -> {

					if(!charFilters.isEmpty()){
						charFilters.clear();
						return persist(s, analyzer);
					};

					return Uni.createFrom().nullItem();

				})));
	}

	public Uni<Tuple2<Analyzer, CharFilter>> addCharFilterToAnalyzer(long id, long charFilterId) {

		return sessionFactory.withTransaction((s, tr) -> findById(s, id)
			.onItem()
			.ifNotNull()
			.transformToUni(analyzer -> _charFilterService.findById(s, charFilterId)
				.onItem()
				.ifNotNull()
				.transformToUni(charFilter ->
					s.fetch(analyzer.getCharFilters())
						.onItem()
						.ifNotNull()
						.transformToUni(charFilters -> {

							if (charFilters.add(charFilter)) {

								analyzer.setCharFilters(charFilters);

								return persist(s, analyzer)
									.map(newSC -> Tuple2.of(newSC, charFilter));
							}

							return Uni.createFrom().nullItem();

						})
				)
			));
	}

	public Uni<Tuple2<Analyzer, CharFilter>> removeCharFilterFromAnalyzer(
		long id, long charFilterId) {
		return sessionFactory.withTransaction((s, tr) -> findById(s, id)
			.onItem()
			.ifNotNull()
			.transformToUni(analyzer -> s.fetch(analyzer.getCharFilters())
				.onItem()
				.ifNotNull()
				.transformToUni(charFilters -> {

					if (analyzer.removeCharFilter(charFilters, charFilterId)) {

						return persist(s, analyzer)
							.map(newSC -> Tuple2.of(newSC, null));
					}

					return Uni.createFrom().nullItem();

				})));
	}

	public Uni<Tuple2<Analyzer, Tokenizer>> bindTokenizer(long analyzerId, long tokenizerId) {
		return sessionFactory.withTransaction((s, tr) -> findById(s, analyzerId)
			.onItem()
			.ifNotNull()
			.transformToUni(analyzer -> _tokenizerService.findById(s, tokenizerId)
				.onItem()
				.ifNotNull()
				.transformToUni(tokenizer -> {
					analyzer.setTokenizer(tokenizer);
					return persist(s, analyzer).map(t -> Tuple2.of(t, tokenizer));
				})));
	}

	public Uni<Tuple2<Analyzer, Tokenizer>> unbindTokenizer(long analyzerId) {
		return sessionFactory.withTransaction((s, tr) -> findById(s, analyzerId)
			.onItem()
			.ifNotNull()
			.transformToUni(analyzer -> {
				analyzer.setTokenizer(null);
				return persist(s, analyzer).map(t -> Tuple2.of(t, null));
			}));
	}

	public Uni<Void> load(Analyzer analyzer) {
		return sessionFactory.withTransaction(s -> {

			List<Uni<?>> unis = new ArrayList<>();

			unis.add(s.fetch(analyzer.getTokenizer()));
			unis.add(s.fetch(analyzer.getCharFilters()));
			unis.add(s.fetch(analyzer.getTokenFilters()));

			return Uni.combine().all().unis(unis).collectFailures().discardItems();

		});
	}


	@Inject
	TokenFilterService _tokenFilterService;
	@Inject
	TokenizerService _tokenizerService;
	@Inject
	CharFilterService _charFilterService;


}
