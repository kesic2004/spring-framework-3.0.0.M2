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

package org.springframework.expression.spel.support;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.util.ClassUtils;
import org.springframework.util.NumberUtils;

/**
 * @author Juergen Hoeller
 * @since 3.0
 */
public class StandardTypeConverter implements TypeConverter {

	@SuppressWarnings("unchecked")
	public <T> T convertValue(Object value, Class<T> targetType) throws EvaluationException {
		if (ClassUtils.isAssignableValue(targetType, value)) {
			return (T) value;
		}
		if (String.class.equals(targetType)) {
			return (T) (value != null ? value.toString() : null);
		}
		Class actualTargetType = ClassUtils.resolvePrimitiveIfNecessary(targetType);
		if (Number.class.isAssignableFrom(actualTargetType)) {
			try {
				if (value instanceof String) {
					return (T) NumberUtils.parseNumber(value.toString(), (Class<Number>) actualTargetType);
				}
				else if (value instanceof Number) {
					return (T) NumberUtils.convertNumberToTargetClass((Number) value, (Class<Number>) actualTargetType);
				}
			}
			catch (IllegalArgumentException ex) {
				throw new SpelException(SpelMessages.PROBLEM_DURING_TYPE_CONVERSION, ex.getMessage());
			}
		}
		if (Character.class.equals(actualTargetType)) {
			if (value instanceof String) {
				String str = (String) value;
				if (str.length() == 1) {
					return (T) new Character(str.charAt(0));
				}
			}
			else if (value instanceof Number) {
				return (T) new Character((char) ((Number) value).shortValue());
			}
		}
		if (Boolean.class.equals(actualTargetType) && value instanceof String) {
			String str = (String) value;
			if ("true".equalsIgnoreCase(str)) {
				return (T) Boolean.TRUE;
			}
			else if ("false".equalsIgnoreCase(str)) {
				return (T) Boolean.FALSE;
			}
		}
		throw new SpelException(SpelMessages.TYPE_CONVERSION_ERROR, value.getClass(), targetType);
	}

	public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		if (ClassUtils.isAssignable(targetType, sourceType) || String.class.equals(targetType)) {
			return true;
		}
		Class actualTargetType = ClassUtils.resolvePrimitiveIfNecessary(targetType);
		return (((Number.class.isAssignableFrom(actualTargetType) || Character.class.equals(actualTargetType)) &&
				(String.class.equals(sourceType) || Number.class.isAssignableFrom(sourceType))) ||
				(Boolean.class.equals(actualTargetType) && String.class.equals(sourceType)));
	}

}
