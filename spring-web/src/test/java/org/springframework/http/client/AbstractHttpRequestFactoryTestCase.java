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
import java.net.URI;
import java.util.Arrays;
import java.util.Enumeration;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import org.springframework.util.FileCopyUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

public abstract class AbstractHttpRequestFactoryTestCase {

	private ClientHttpRequestFactory factory;

	private static Server jettyServer;

	@BeforeClass
	public static void startJettyServer() throws Exception {
		jettyServer = new Server(8889);
		Context jettyContext = new Context(jettyServer, "/");
		jettyContext.addServlet(new ServletHolder(new EchoServlet()), "/echo");
		jettyContext.addServlet(new ServletHolder(new ErrorServlet(404)), "/errors/notfound");
		jettyServer.start();
	}

	@Before
	public final void createFactory() {
		factory = createRequestFactory();
	}

	protected abstract ClientHttpRequestFactory createRequestFactory();

	@AfterClass
	public static void stopJettyServer() throws Exception {
		if (jettyServer != null) {
			jettyServer.stop();
		}
	}

	@Test
	public void status() throws Exception {
		ClientHttpRequest request =
				factory.createRequest(new URI("http://localhost:8889/errors/notfound"), HttpMethod.GET);
		assertEquals("Invalid HTTP method", HttpMethod.GET, request.getMethod());
		ClientHttpResponse response = request.execute();
		assertEquals("Invalid status code", HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@Test
	public void echo() throws Exception {
		ClientHttpRequest request = factory.createRequest(new URI("http://localhost:8889/echo"), HttpMethod.PUT);
		assertEquals("Invalid HTTP method", HttpMethod.PUT, request.getMethod());
		String headerName = "MyHeader";
		String headerValue1 = "value1";
		request.getHeaders().add(headerName, headerValue1);
		String headerValue2 = "value2";
		request.getHeaders().add(headerName, headerValue2);
		byte[] body = "Hello World".getBytes("UTF-8");
		FileCopyUtils.copy(body, request.getBody());
		ClientHttpResponse response = request.execute();
		assertEquals("Invalid status code", HttpStatus.OK, response.getStatusCode());
		assertTrue("Header not found", response.getHeaders().containsKey(headerName));
		assertEquals("Header value not found", Arrays.asList(headerValue1, headerValue2),
				response.getHeaders().get(headerName));
		byte[] result = FileCopyUtils.copyToByteArray(response.getBody());
		assertTrue("Invalid body", Arrays.equals(body, result));
	}

	@Test(expected = IllegalStateException.class)
	public void multipleWrites() throws Exception {
		ClientHttpRequest request = factory.createRequest(new URI("http://localhost:8889/echo"), HttpMethod.POST);
		byte[] body = "Hello World".getBytes("UTF-8");
		FileCopyUtils.copy(body, request.getBody());
		request.execute();
		FileCopyUtils.copy(body, request.getBody());
	}

	@Test(expected = IllegalStateException.class)
	public void headersAfterExecute() throws Exception {
		ClientHttpRequest request = factory.createRequest(new URI("http://localhost:8889/echo"), HttpMethod.POST);
		request.getHeaders().add("MyHeader", "value");
		byte[] body = "Hello World".getBytes("UTF-8");
		FileCopyUtils.copy(body, request.getBody());
		request.execute();
		request.getHeaders().add("MyHeader", "value");
	}

	/**
	 * Servlet that returns and error message for a given status code.
	 */
	private static class ErrorServlet extends GenericServlet {

		private final int sc;

		private ErrorServlet(int sc) {
			this.sc = sc;
		}

		@Override
		public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
			((HttpServletResponse) response).sendError(sc);
		}
	}

	private static class EchoServlet extends HttpServlet {

		@Override
		protected void doPut(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			echo(request, response);
		}

		@Override
		protected void doPost(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			echo(request, response);
		}

		private void echo(HttpServletRequest request, HttpServletResponse response) throws IOException {
			response.setStatus(HttpServletResponse.SC_OK);
			for (Enumeration e1 = request.getHeaderNames(); e1.hasMoreElements();) {
				String headerName = (String) e1.nextElement();
				for (Enumeration e2 = request.getHeaders(headerName); e2.hasMoreElements();) {
					String headerValue = (String) e2.nextElement();
					response.addHeader(headerName, headerValue);
				}
			}
			FileCopyUtils.copy(request.getInputStream(), response.getOutputStream());
		}
	}

}