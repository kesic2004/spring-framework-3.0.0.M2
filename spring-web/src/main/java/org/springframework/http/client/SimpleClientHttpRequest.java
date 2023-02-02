/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import org.springframework.util.FileCopyUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * {@link ClientHttpRequest} implementation that uses standard J2SE facilities to execute requests.
 * Created via the {@link SimpleClientHttpRequestFactory}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see SimpleClientHttpRequestFactory#createRequest(java.net.URI, HttpMethod)
 */
final class SimpleClientHttpRequest extends AbstractClientHttpRequest {

	private final HttpURLConnection connection;


	public SimpleClientHttpRequest(HttpURLConnection connection) {
		this.connection = connection;
	}


	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.connection.getRequestMethod());
	}

	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			String headerName = entry.getKey();
			for (String headerValue : entry.getValue()) {
				this.connection.addRequestProperty(headerName, headerValue);
			}
		}
		this.connection.connect();
		FileCopyUtils.copy(bufferedOutput, this.connection.getOutputStream());
		return new SimpleClientHttpResponse(this.connection);
	}

}
