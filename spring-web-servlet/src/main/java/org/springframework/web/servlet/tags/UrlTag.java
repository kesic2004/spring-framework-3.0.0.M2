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

package org.springframework.web.servlet.tags;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.springframework.util.StringUtils;
import org.springframework.web.util.TagUtils;

/**
 * JSP tag for creating URLs. Modeled after the JSTL c:url tag with backwards
 * compatibility in mind.
 * 
 * <p>Enhancements to the JSTL functionality include:
 * <ul>
 * <li>URL encoded template URI variables</li>
 * <li>XML escaping of URLs</li>
 * </ul>
 * 
 * <p>Template URI variables are indicated in the {@link #setValue(String) 'value'}
 * attribute and marked by braces '{variableName}'. The braces and attribute name are
 * replaced by the URL encoded value of a parameter defined with the spring:param tag 
 * in the body of the url tag. If no parameter is available the literal value is 
 * passed through. Params matched to template variables will not be added to the query 
 * string.
 * 
 * <p>Use of the spring:param tag for URI template variables is strongly recommended 
 * over direct EL substitution as the values are URL encoded.  Failure to properly 
 * encode URL can leave an application vulnerable to XSS and other injection attacks.
 * 
 * <p>URLs can be XML escaped by setting the {@link #setEscapeXml(String) 
 * 'escapeXml'} attribute to 'true', the default is 'false'.
 * 
 * <p>Example usage:
 * <pre>&lt;spring:url value="/url/path/{variableName}"&gt;
 *   &lt;spring:param name="variableName" value="more than JSTL c:url" /&gt;
 * &lt;/spring:url&gt;</pre>
 * Results in:
 * <code>/currentApplicationContext/url/path/more+than+JSTL+c%3Aurl</code>
 * 
 * @author Scott Andrews
 * @since 3.0
 * @see ParamTag
 */
public class UrlTag extends TagSupport implements ParamAware {

	private static final String URL_TEMPLATE_DELIMITER_PREFIX = "{";

	private static final String URL_TEMPLATE_DELIMITER_SUFFIX = "}";

	private static final String URL_TYPE_ABSOLUTE = "://";

	private static final char[] XML_CHARS = { '&', '<', '>', '"', '\'' };


	private List<Param> params;

	private Set<String> templateParams;

	private UrlType type;

	private String value;

	private String context;

	private String var;

	private int scope = PageContext.PAGE_SCOPE;

	private boolean escapeXml = false;


	/**
	 * Sets the value of the URL
	 */
	public void setValue(String value) {
		if (value.contains(URL_TYPE_ABSOLUTE)) {
			this.type = UrlType.ABSOLUTE;
			this.value = value;
		}
		else if (value.startsWith("/")) {
			this.type = UrlType.CONTEXT_RELATIVE;
			this.value = value;
		}
		else {
			this.type = UrlType.RELATIVE;
			this.value = value;
		}
	}

	/**
	 * Set the context path for the URL. Defaults to the current context
	 */
	public void setContext(String context) {
		if (context.startsWith("/")) {
			this.context = context;
		}
		else {
			this.context = "/" + context;
		}
	}

	/**
	 * Set the variable name to expose the URL under. Defaults to rendering the
	 * URL to the current JspWriter
	 */
	public void setVar(String var) {
		this.var = var;
	}

	/**
	 * Set the scope to export the URL variable to. This attribute has no
	 * meaning unless var is also defined.
	 */
	public void setScope(String scope) {
		this.scope = TagUtils.getScope(scope);
	}

	/**
	 * Instructs the tag to XML entity encode the resulting URL.
	 * <p>Defaults to false to maintain compatibility with the JSTL c:url tag.
	 * <p><b>NOTE:</b> Strongly recommended to set as 'true' when rendering
	 * directly to the JspWriter in an XML or HTML based file.
	 */
	public void setEscapeXml(String escapeXml) {
		this.escapeXml = Boolean.valueOf(escapeXml);
	}

	public void addParam(Param param) {
		this.params.add(param);
	}


	@Override
	public int doStartTag() throws JspException {
		this.params = new LinkedList<Param>();
		this.templateParams = new HashSet<String>();
		return EVAL_BODY_INCLUDE;
	}

	@Override
	public int doEndTag() throws JspException {
		String url = createUrl();
		if (this.var == null) {
			// print the url to the writer
			try {
				pageContext.getOut().print(url);
			}
			catch (IOException e) {
				throw new JspException(e);
			}
		}
		else {
			// store the url as a variable
			pageContext.setAttribute(var, url, scope);
		}
		return EVAL_PAGE;
	}


	/**
	 * Build the URL for the tag from the tag attributes and parameters.
	 * @return the URL value as a String
	 * @throws JspException
	 */
	private String createUrl() throws JspException {
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
		HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();
		StringBuilder url = new StringBuilder();
		if (this.type == UrlType.CONTEXT_RELATIVE) {
			// add application context to url
			if (this.context == null) {
				url.append(request.getContextPath());
			}
			else {
				url.append(this.context);
			}
		}
		if (this.type != UrlType.RELATIVE && this.type != UrlType.ABSOLUTE && !this.value.startsWith("/")) {
			url.append("/");
		}
		url.append(replaceUriTemplateParams(this.value, this.params, this.templateParams));
		url.append(createQueryString(this.params, this.templateParams, (url.indexOf("?") == -1)));

		String urlStr = url.toString();
		if (this.type != UrlType.ABSOLUTE) {
			// Add the session identifier if needed
			// (Do not embed the session identifier in a remote link!)
			urlStr = response.encodeURL(urlStr);
		}
		if (this.escapeXml) {
			urlStr = escapeXml(urlStr);
		}
		return urlStr;
	}

	/**
	 * Build the query string from available parameters that have not already
	 * been applied as template params.
	 * <p>The names and values of parameters are URL encoded.
	 * @param params the parameters to build the query string from
	 * @param usedParams set of parameter names that have been applied as
	 * template params
	 * @param includeQueryStringDelimiter true if the query string should start
	 * with a '?' instead of '&'
	 * @return the query string
	 * @throws JspException
	 */
	protected String createQueryString(
			List<Param> params, Set<String> usedParams, boolean includeQueryStringDelimiter)
			throws JspException {

		StringBuilder qs = new StringBuilder();
		for (Param param : params) {
			if (!usedParams.contains(param.getName()) && param.getName() != null && !"".equals(param.getName())) {
				if (includeQueryStringDelimiter && qs.length() == 0) {
					qs.append("?");
				}
				else {
					qs.append("&");
				}
				qs.append(urlEncode(param.getName()));
				if (param.getValue() != null) {
					qs.append("=");
					qs.append(urlEncode(param.getValue()));
				}
			}
		}
		return qs.toString();
	}

	/**
	 * Replace template markers in the URL matching available parameters. The
	 * name of matched parameters are added to the used parameters set.
	 * <p>Parameter values are URL encoded.
	 * @param uri the URL with template parameters to replace
	 * @param params parameters used to replace template markers
	 * @param usedParams set of template parameter names that have been replaced
	 * @return the URL with template parameters replaced
	 * @throws JspException
	 */
	protected String replaceUriTemplateParams(String uri, List<Param> params, Set<String> usedParams)
			throws JspException {

		for (Param param : params) {
			String template = URL_TEMPLATE_DELIMITER_PREFIX + param.getName() + URL_TEMPLATE_DELIMITER_SUFFIX;
			if (uri.contains(template)) {
				usedParams.add(param.getName());
				uri = uri.replace(template, urlEncode(param.getValue()));
			}
		}
		return uri;
	}

	/**
	 * URL-encode the providedSstring using the character encoding for the response.
	 * @param value the value to encode
	 * @return the URL encoded value
	 * @throws JspException if the character encoding is invalid
	 */
	protected String urlEncode(String value) throws JspException {
		if (value == null) {
			return null;
		}
		try {
			return URLEncoder.encode(value, pageContext.getResponse().getCharacterEncoding());
		}
		catch (UnsupportedEncodingException ex) {
			throw new JspException(ex);
		}
	}

	/**
	 * XML entity encode the provided string. &#38;, &#60;, &#62;, &#39; and
	 * &#34; are encoded to their entity values.
	 * @param xml the value to escape
	 * @return the escaped value
	 */
	protected String escapeXml(String xml) {
		if (xml == null) {
			return null;
		}
		String escapedXml = xml;
		for (char xmlChar : XML_CHARS) {
			escapedXml = StringUtils.replace(escapedXml, String.valueOf(xmlChar), entityValue(xmlChar));
		}
		return escapedXml;
	}

	/**
	 * Convert a character value to a XML entity value.
	 * The decimal value of the character is used.
	 * <p>For example, 'A' is converted to "&amp;#65;".
	 * @param xmlChar the character to encode
	 * @return the entity value
	 */
	protected String entityValue(char xmlChar) {
		return new StringBuilder().append("&#").append(Integer.toString(xmlChar)).append(";").toString();
	}


	/**
	 * Internal enum that classifies URLs by type.
	 */
	private enum UrlType {

		CONTEXT_RELATIVE, RELATIVE, ABSOLUTE
	}

}
