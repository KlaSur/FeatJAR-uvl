/*
 * Copyright (C) 2025 FeatJAR-Development-Team
 *
 * This file is part of FeatJAR-uvl.
 *
 * uvl is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * uvl is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with uvl. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/FeatureIDE/FeatJAR-uvl> for further information.
 */

package de.featjar.feature.model.io.uvl;

import de.featjar.base.data.Result;
import de.featjar.formula.structure.IExpression;
import de.featjar.formula.structure.IFormula;
import de.featjar.formula.structure.connective.And;
import de.featjar.formula.structure.connective.BiImplies;
import de.featjar.formula.structure.connective.Implies;
import de.featjar.formula.structure.connective.Not;
import de.featjar.formula.structure.connective.Or;
import de.featjar.formula.structure.predicate.Equals;
import de.featjar.formula.structure.predicate.GreaterEqual;
import de.featjar.formula.structure.predicate.GreaterThan;
import de.featjar.formula.structure.predicate.LessEqual;
import de.featjar.formula.structure.predicate.LessThan;
import de.featjar.formula.structure.predicate.Literal;
import de.featjar.formula.structure.predicate.NotEquals;
import de.featjar.formula.structure.term.ITerm;
import de.featjar.formula.structure.term.IfThenElse;
import de.featjar.formula.structure.term.function.integer.IntegerAdd;
import de.featjar.formula.structure.term.value.Constant;
import de.vill.model.building.VariableReference;
import de.vill.model.constraint.AndConstraint;
import de.vill.model.constraint.EqualEquationConstraint;
import de.vill.model.constraint.EquivalenceConstraint;
import de.vill.model.constraint.GreaterEqualsEquationConstraint;
import de.vill.model.constraint.GreaterEquationConstraint;
import de.vill.model.constraint.ImplicationConstraint;
import de.vill.model.constraint.LiteralConstraint;
import de.vill.model.constraint.LowerEqualsEquationConstraint;
import de.vill.model.constraint.LowerEquationConstraint;
import de.vill.model.constraint.NotConstraint;
import de.vill.model.constraint.NotEqualsEquationConstraint;
import de.vill.model.constraint.OrConstraint;
import de.vill.model.constraint.ParenthesisConstraint;
import de.vill.model.expression.AddExpression;
import de.vill.model.expression.Expression;
import de.vill.model.expression.LiteralExpression;
import de.vill.model.expression.NumberExpression;

public class UVLConstraintParser {
	public Result<ITerm> parseExpressionConstraint(Expression expression) {
		if (expression instanceof LiteralExpression) {
			LiteralExpression literalExpression = (LiteralExpression) expression;
			VariableReference content = literalExpression.getContent();
			
			if (content instanceof de.vill.model.Attribute) {
				de.vill.model.Attribute uvlAttribute = (de.vill.model.Attribute) content;
		    	de.vill.model.Feature feature = uvlAttribute.getFeature();
		    	
		    	IFormula condition = new Literal(feature.getFeatureName());
		    	
		    	Object defaultValue = new Object();
		    	switch (uvlAttribute.getType()) {
		    		case "string":
		    			defaultValue = "";
		    			break;
		    		case "boolean":
		    			defaultValue = Boolean.TRUE;
		    			break;
		    		case "number":
		    			defaultValue = 0l;
		    			break;
		    	}
		    	
		    	Constant term1 = new Constant(uvlAttribute.getValue());
		    	Constant term2 = new Constant(defaultValue);
		    	
		    	return Result.of(new IfThenElse(condition, term1, term2));
			}
		} else if (expression instanceof NumberExpression) {
			NumberExpression numberExpression = (NumberExpression) expression;
			return Result.of(new Constant(numberExpression.getNumber()));
		} else if (expression instanceof AddExpression) {
			AddExpression addExpression = (AddExpression) expression;
			return Result.of(new IntegerAdd(parseExpressionConstraint(addExpression.getLeft()).get(), 
					parseExpressionConstraint(addExpression.getRight()).get()));
		}
		
		
	
		
		
		Constant a = new Constant(0);
		Constant b = new Constant(0);
		return Result.of(new IntegerAdd(a, b));
	}
	
	public Result<IExpression> parse(de.vill.model.constraint.Constraint uvlConstraint) {
		try {
		    Result<IExpression> featureModelConstraint = parseUVLConstraintRecursively(uvlConstraint); 
		    return featureModelConstraint;
		} catch (RuntimeException e) {
		    return Result.empty();
		}
	}
	
	public Result<IExpression> parseUVLConstraintRecursively(de.vill.model.constraint.Constraint uvlConstraint) throws RuntimeException {
		if (uvlConstraint instanceof LiteralConstraint) {
			LiteralConstraint literalConstraint = (LiteralConstraint) uvlConstraint;
			VariableReference variableReference = literalConstraint.getReference();
		    if (variableReference instanceof de.vill.model.Feature) {
		    	de.vill.model.Feature uvlFeature = (de.vill.model.Feature) variableReference;
		    	return Result.of(new Literal(uvlFeature.getFeatureName()));
		    }
		    
		    if (variableReference instanceof de.vill.model.Attribute) {
		    	de.vill.model.Attribute uvlAttribute = (de.vill.model.Attribute) variableReference;
		    	de.vill.model.Feature feature = uvlAttribute.getFeature();
		    	
		    	IFormula condition = new Literal(feature.getFeatureName());
		    	
		    	Object defaultValue = new Object();
		    	switch (uvlAttribute.getType()) {
		    		case "string":
		    			defaultValue = "";
		    			break;
		    		case "boolean":
		    			defaultValue = Boolean.TRUE;
		    			break;
		    		case "number":
		    			defaultValue = 0l;
		    			break;
		    	}
		    	
		    	Constant term1 = new Constant(uvlAttribute.getValue());
		    	Constant term2 = new Constant(defaultValue);
		    	
		    	return Result.of(new IfThenElse(condition, term1, term2));
		    }
		} else if (uvlConstraint instanceof ParenthesisConstraint) {
			ParenthesisConstraint parenthesisConstraint = (ParenthesisConstraint) uvlConstraint;
			parseUVLConstraintRecursively(parenthesisConstraint.getContent());
		} else if (uvlConstraint instanceof ImplicationConstraint) {
			ImplicationConstraint implicationConstraint = (ImplicationConstraint) uvlConstraint;
			return Result.of(new Implies((IFormula) parse(implicationConstraint.getLeft()).get(), 
					(IFormula) parse(implicationConstraint.getRight()).get()));
		} else if (uvlConstraint instanceof NotConstraint) {
			NotConstraint notConstraint = (NotConstraint) uvlConstraint;
			return Result.of(new Not((IFormula) parse(notConstraint.getContent()).get()));
		} else if (uvlConstraint instanceof AndConstraint) {
			AndConstraint andConstraint = (AndConstraint) uvlConstraint;
			return Result.of(new And((IFormula) parse(andConstraint.getLeft()).get(), 
					(IFormula) parse(andConstraint.getRight()).get()));	
		} else if (uvlConstraint instanceof OrConstraint) {
			OrConstraint orConstraint = (OrConstraint) uvlConstraint;
			return Result.of(new Or((IFormula) parse(orConstraint.getLeft()).get(), 
					(IFormula) parse(orConstraint.getRight()).get()));	
		} else if (uvlConstraint instanceof EqualEquationConstraint) {
			EqualEquationConstraint equalConstraint = (EqualEquationConstraint) uvlConstraint;
			return Result.of(new Equals(parseExpressionConstraint(equalConstraint.getLeft()).get(), 
					parseExpressionConstraint(equalConstraint.getRight()).get()));
		} else if (uvlConstraint instanceof EquivalenceConstraint) {
			EquivalenceConstraint equivalenceConstraint = (EquivalenceConstraint) uvlConstraint;
			return Result.of(new BiImplies((IFormula) parse(equivalenceConstraint.getLeft()).get(), 
					(IFormula) parse(equivalenceConstraint.getRight()).get()));
		} else if (uvlConstraint instanceof LowerEqualsEquationConstraint) {
			LowerEqualsEquationConstraint lowerEqualsConstraint = (LowerEqualsEquationConstraint) uvlConstraint;
			return Result.of(new LessEqual(parseExpressionConstraint(lowerEqualsConstraint.getLeft()).get(), 
					parseExpressionConstraint(lowerEqualsConstraint.getRight()).get()));
		} else if (uvlConstraint instanceof GreaterEqualsEquationConstraint) {
			GreaterEqualsEquationConstraint greaterEqualConstraint = (GreaterEqualsEquationConstraint) uvlConstraint;
			return Result.of(new GreaterEqual(parseExpressionConstraint(greaterEqualConstraint.getLeft()).get(), 
					parseExpressionConstraint(greaterEqualConstraint.getRight()).get()));
		} else if (uvlConstraint instanceof NotEqualsEquationConstraint) {
			NotEqualsEquationConstraint notEqualsConstraint = (NotEqualsEquationConstraint) uvlConstraint;
			return Result.of(new NotEquals(parseExpressionConstraint(notEqualsConstraint.getLeft()).get(), 
					parseExpressionConstraint(notEqualsConstraint.getRight()).get()));
		} else if (uvlConstraint instanceof LowerEquationConstraint) {
			LowerEquationConstraint lowerConstraint = (LowerEquationConstraint) uvlConstraint;
			return Result.of(new LessThan(parseExpressionConstraint(lowerConstraint.getLeft()).get(), 
					parseExpressionConstraint(lowerConstraint.getRight()).get()));
		} else if (uvlConstraint instanceof GreaterEquationConstraint) {
			GreaterEquationConstraint greaterConstraint = (GreaterEquationConstraint) uvlConstraint;
			return Result.of(new GreaterThan(parseExpressionConstraint(greaterConstraint.getLeft()).get(), 
					parseExpressionConstraint(greaterConstraint.getRight()).get()));
		} 
		
		throw new RuntimeException();
	}
}