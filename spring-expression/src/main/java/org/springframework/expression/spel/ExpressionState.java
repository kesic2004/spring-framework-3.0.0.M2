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
package org.springframework.expression.spel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeComparator;

/**
 * An ExpressionState is for maintaining per-expression-evaluation state, any changes to it are not seen by other
 * expressions but it gives a place to hold local variables and for component expressions in a compound expression to
 * communicate state. This is in contrast to the EvaluationContext, which is shared amongst expression evaluations, and
 * any changes to it will be seen by other expressions or any code that chooses to ask questions of the context.
 * 
 * It also acts as a place for to define common utility routines that the various Ast nodes might need.
 * 
 * @author Andy Clement
 * @since 3.0
 */
public class ExpressionState {

	private final EvaluationContext relatedContext;

	private final Stack<VariableScope> variableScopes = new Stack<VariableScope>();

	private final Stack<Object> contextObjects = new Stack<Object>();


	public ExpressionState() {
		this(null);
	}

	public ExpressionState(EvaluationContext context) {
		this.relatedContext = context;
		createVariableScope();
	}


	private void createVariableScope() {
		this.variableScopes.add(new VariableScope()); // create an empty top level VariableScope
	}

	/**
	 * The active context object is what unqualified references to properties/etc are resolved against.
	 */
	public Object getActiveContextObject() {
		if (this.contextObjects.isEmpty()) {
			return this.relatedContext.getRootObject();
		}
		return this.contextObjects.peek();
	}

	public void pushActiveContextObject(Object obj) {
		this.contextObjects.push(obj);
	}

	public void popActiveContextObject() {
		this.contextObjects.pop();
	}

	public Object getRootContextObject() {
		return this.relatedContext.getRootObject();
	}

	public void setVariable(String name, Object value) {
		this.relatedContext.setVariable(name, value);
	}

	public Object lookupVariable(String name) {
		return this.relatedContext.lookupVariable(name);
	}

	public Object lookupReference(Object contextName, String objectName) throws EvaluationException {
		return this.relatedContext.lookupReference(contextName, objectName);
	}

	public TypeComparator getTypeComparator() {
		return this.relatedContext.getTypeComparator();
	}

	public Class<?> findType(String type) throws EvaluationException {
		return this.relatedContext.getTypeLocator().findType(type);
	}

	public <T> T convertValue(Object value, Class<T> targetType) throws EvaluationException {
		return this.relatedContext.getTypeConverter().convertValue(value, targetType);
	}

	/**
	 * A new scope is entered when a function is invoked
	 */
	public void enterScope(Map<String, Object> argMap) {
		this.variableScopes.push(new VariableScope(argMap));
	}

	public void enterScope(String name, Object value) {
		this.variableScopes.push(new VariableScope(name, value));
	}

	public void exitScope() {
		this.variableScopes.pop();
	}

	public void setLocalVariable(String name, Object value) {
		this.variableScopes.peek().setVariable(name, value);
	}

	public Object lookupLocalVariable(String name) {
		int scopeNumber = this.variableScopes.size() - 1;
		for (int i = scopeNumber; i >= 0; i--) {
			if (this.variableScopes.get(i).definesVariable(name)) {
				return this.variableScopes.get(i).lookupVariable(name);
			}
		}
		return null;
	}

	public Object operate(Operation op, Object left, Object right) throws EvaluationException {
		OperatorOverloader overloader = this.relatedContext.getOperatorOverloader();
		if (overloader.overridesOperation(op, left, right)) {
			return overloader.operate(op, left, right);
		}
		else {
			throw new SpelException(SpelMessages.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES, op, left, right);
		}
	}

	public List<PropertyAccessor> getPropertyAccessors() {
		return this.relatedContext.getPropertyAccessors();
	}

	public EvaluationContext getEvaluationContext() {
		return this.relatedContext;
	}


	/**
	 * A new scope is entered when a function is called and it is used to hold the parameters to the function call.  If the names
	 * of the parameters clash with those in a higher level scope, those in the higher level scope will not be accessible whilst
	 * the function is executing.  When the function returns the scope is exited.
	 *
	 */
	private static class VariableScope {

		private final Map<String, Object> vars = new HashMap<String, Object>();

		public VariableScope() { }

		public VariableScope(Map<String, Object> arguments) {
			if (arguments != null) {
				this.vars.putAll(arguments);
			}
		}
		
		public VariableScope(String name, Object value) {
			this.vars.put(name,value);
		}

		public Object lookupVariable(String name) {
			return this.vars.get(name);
		}

		public void setVariable(String name, Object value) {
			this.vars.put(name,value);
		}

		public boolean definesVariable(String name) {
			return this.vars.containsKey(name);
		}
	}

}
