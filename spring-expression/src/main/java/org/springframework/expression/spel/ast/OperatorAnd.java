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

package org.springframework.expression.spel.ast;

import org.antlr.runtime.Token;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.ExpressionState;

/**
 * Represents the boolean AND operation.
 *
 * @author Andy Clement
 * @since 3.0
 */
public class OperatorAnd extends Operator {

	public OperatorAnd(Token payload) {
		super(payload);
	}

	@Override
	public String getOperatorName() {
		return "and";
	}

	@Override
	public Object getValueInternal(ExpressionState state) throws EvaluationException {
		boolean leftValue;
		boolean rightValue;

		try {
			leftValue = state.convertValue(getLeftOperand().getValueInternal(state), Boolean.class);
		}
		catch (SpelException ee) {
			ee.setPosition(getLeftOperand().getCharPositionInLine());
			throw ee;
		}

		if (leftValue == false) {
			return false; // no need to evaluate right operand
		}

		try {
			rightValue = state.convertValue(getRightOperand().getValueInternal(state), Boolean.class);
		}
		catch (SpelException ee) {
			ee.setPosition(getRightOperand().getCharPositionInLine());
			throw ee;
		}

		return /* leftValue && */rightValue;
	}

}
