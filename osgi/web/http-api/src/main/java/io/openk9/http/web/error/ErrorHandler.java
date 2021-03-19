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

package io.openk9.http.web.error;

import io.openk9.http.exception.HttpException;
import io.openk9.http.web.HttpResponse;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.BiFunction;

public interface ErrorHandler extends
	BiFunction<Throwable, HttpResponse, Publisher<Void>> {

	ErrorHandler DEFAULT = new ErrorHandler() {
		@Override
		public Publisher<Void> apply(
			Throwable throwable, HttpResponse httpResponse) {

			if (_log.isErrorEnabled()) {
				_log.error(throwable.getMessage(), throwable);
			}

			if (throwable instanceof HttpException) {
				HttpException httpException = (HttpException) throwable;

				Publisher<String> body = httpException.getBody();

				String status = httpResponse.status(
					httpException.getStatusCode(),
					httpException.getReason());

				return httpResponse.sendString(
					Objects.requireNonNullElseGet(
						body,
						() -> Mono.just(status)));

			}


			return httpResponse.sendString(
				Mono.just(
					httpResponse.status(500, throwable.getMessage()))
			);
		}

		private final Logger _log = LoggerFactory.getLogger(
			ErrorHandler.class);

	};

	default Class<? extends Throwable> exceptionType() {
		return Throwable.class;
	}

}
