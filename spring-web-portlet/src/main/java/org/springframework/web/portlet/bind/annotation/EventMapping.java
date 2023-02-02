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

package org.springframework.web.portlet.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.web.bind.annotation.Mapping;

/**
 * Annotation for mapping Portlet event requests onto handler methods.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.web.bind.annotation.RequestMapping
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface EventMapping {

	/**
	 * The name of the event to be handled.
	 * <p>Typically the local name of the event, but fully qualified names
	 * with a "{...}" namespace part will be mapped correctly as well.
	 * <p>If not specified, the render method will be invoked for any
	 * event request within its general mapping.
	 * @see javax.portlet.EventRequest#getEvent()
	 * @see javax.portlet.Event#getName()
	 */
	String value();

	/**
	 * The parameters of the mapped request, narrowing the primary mapping.
	 * <p>Same format for any environment: a sequence of "myParam=myValue" style
	 * expressions, with a request only mapped if each such parameter is found
	 * to have the given value. "myParam" style expressions are also supported,
	 * with such parameters having to be present in the request (allowed to have
	 * any value). Finally, "!myParam" style expressions indicate that the
	 * specified parameter is <i>not</i> supposed to be present in the request.
	 * @see org.springframework.web.bind.annotation.RequestMapping#params()
	 */
	String[] params() default {};

}
