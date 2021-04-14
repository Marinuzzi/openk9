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

import { useCallback, useEffect } from "react";
import create from "zustand";
import { devtools } from "zustand/middleware";
import { useHistory, useParams } from "react-router-dom";

import {
  isSearchQueryEmpty,
  SearchQuery,
  SearchResult,
  doSearch,
  SearchRequest,
  despaceString,
  undespaceString,
  InputSuggestionToken,
  getTokenSuggestions,
  PluginInfo,
  getPlugins,
  TenantJSONConfig,
  emptyTenantJSONConfig,
  getTenants,
} from "@openk9/http-api";

const resultsChunkNumber = 8;
const timeoutDebounce = 500;
const suggTimeoutDebounce = 300;

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

export type StateType = {
  initial: boolean;
  searchQuery: SearchQuery;
  setSearchQuery(query: SearchQuery): void;
  lastSearch: number;
  results: SearchResult<{}> | null;
  range: [number, number] | null;
  loading: boolean;
  doLoadMore(): void;
  focus: "INPUT" | "RESULTS";
  suggestionsRequestTime: number;
  suggestions: InputSuggestionToken[];
  setSuggestions(suggestions: InputSuggestionToken[]): void;
  fetchSuggestions(): Promise<void>;
  focusToken: number | null;
  setFocusToken(focusToken: number | null): void;
  setFocus(focus: "INPUT" | "RESULTS"): void;
  selectedResult: string | null;
  setSelectedResult(selectedResult: string | null): void;
  pluginInfos: PluginInfo[];
  tenantConfig: TenantJSONConfig;
  loadInitial(): Promise<void>;
};

export const useStore = create<StateType>(
  devtools((set, get) => ({
    initial: true,
    lastSearch: 0,
    results: null,
    searchQuery: [],
    loading: false,
    range: null,
    focus: "INPUT",
    suggestionsRequestTime: 0,
    suggestions: [],
    focusToken: null,
    selectedResult: null,
    pluginInfos: [],
    tenantConfig: emptyTenantJSONConfig,

    async loadInitial() {
      const pluginInfos = await getPlugins();

      // TODO getCurrentTenantConfig
      const tenants = await getTenants();
      const tenant = tenants.find(
        (tenant) => window.location.host == tenant.virtualHost,
      );
      const tenantConfig =
        (tenant?.jsonConfig && JSON.parse(tenant?.jsonConfig)) ||
        emptyTenantJSONConfig;

      set((state) => ({ ...state, pluginInfos, tenantConfig }));
    },

    async setSearchQuery(searchQuery: SearchQuery) {
      set((state) => ({
        ...state,
        searchQuery,
        initial: false,
      }));
      get().fetchSuggestions();

      const startTime = new Date().getTime();
      const lastTime = get().lastSearch;
      set((state) => ({
        ...state,
        lastSearch: startTime,
      }));
      if (startTime - lastTime <= timeoutDebounce) {
        await sleep(timeoutDebounce);
      }
      if (get().lastSearch <= startTime) {
        set((state) => ({
          ...state,
          loading: true,
          lastSearch: startTime,
        }));
        const request: SearchRequest = {
          searchQuery,
          range: [0, resultsChunkNumber],
        };
        const results = await doSearch(request);
        set((state) => ({
          ...state,
          results: isSearchQueryEmpty(searchQuery) ? null : results,
          loading: false,
          range: [0, resultsChunkNumber],
        }));
      }
    },

    async doLoadMore() {
      const prev = get();
      if (prev.range) {
        set((state) => ({ ...state, loading: true }));
        const request: SearchRequest = {
          searchQuery: prev.searchQuery,
          range: [prev.range[0], prev.range[1] + resultsChunkNumber],
        };
        const results = await doSearch(request);
        set((state) => ({
          ...state,
          results,
          loading: false,
          range: request.range,
        }));
      }
    },

    setFocus(focus: "INPUT" | "RESULTS") {
      set((state) => ({ ...state, focus }));
    },
    setSuggestions(suggestions: InputSuggestionToken[]) {
      set((state) => ({ ...state, suggestions }));
    },
    setFocusToken(focusToken: number | null) {
      set((state) => ({ ...state, focusToken }));
      get().fetchSuggestions();
    },
    setSelectedResult(selectedResult: string | null) {
      set((state) => ({ ...state, selectedResult }));
    },

    async fetchSuggestions() {
      const startTime = new Date().getTime();
      const token =
        get().focusToken !== null && get().searchQuery[get().focusToken || 0];
      const lastSuggestionsRequestTime = get().suggestionsRequestTime;
      set((state) => ({ ...state, suggestionsRequestTime: startTime }));
      if (startTime - lastSuggestionsRequestTime <= suggTimeoutDebounce) {
        await sleep(suggTimeoutDebounce);
      }
      if (token && get().suggestionsRequestTime <= startTime) {
        const suggestions = await getTokenSuggestions(token);
        if (get().suggestionsRequestTime === startTime) {
          set((state) => ({ ...state, suggestions }));
        }
      } else {
        set((state) => ({ ...state, suggestions: [] }));
      }
    },
  })),
);

export function useSearchQuery() {
  const params = useParams<{ query: string }>();
  const history = useHistory();

  const storedSearchQuery = useStore((s) => s.searchQuery);
  const storedSetSearchQuery = useStore((s) => s.setSearchQuery);

  useEffect(() => {
    try {
      const queryString = undespaceString(params.query || "");
      if (queryString.length > 0) {
        storedSetSearchQuery(JSON.parse(queryString));
      }
    } catch (err) {
      console.warn(err);
    }
  }, [storedSetSearchQuery, params]);

  const setSearchQuery = useCallback(
    (searchQuery: SearchQuery) => {
      storedSetSearchQuery(searchQuery);
      history.push("/q/" + despaceString(JSON.stringify(searchQuery)));
    },
    [history, storedSetSearchQuery],
  );

  return [storedSearchQuery || [], setSearchQuery] as const;
}
