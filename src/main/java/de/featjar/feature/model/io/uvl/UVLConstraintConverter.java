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

import java.util.ArrayList;
import java.util.List;

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
import de.featjar.formula.structure.term.function.integer.IntegerAdd;
import de.featjar.formula.structure.term.function.integer.IntegerDivide;
import de.featjar.formula.structure.term.function.integer.IntegerMultiply;
import de.featjar.formula.structure.term.function.string.StringLength;
import de.featjar.formula.structure.term.value.Constant;
import de.featjar.formula.structure.term.value.Variable;
import de.vill.model.FeatureType;
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
import de.vill.model.constraint.MultiOrConstraint;
import de.vill.model.constraint.NotConstraint;
import de.vill.model.constraint.NotEqualsEquationConstraint;
import de.vill.model.constraint.OrConstraint;
import de.vill.model.constraint.ParenthesisConstraint;
import de.vill.model.expression.AddExpression;
import de.vill.model.expression.DivExpression;
import de.vill.model.expression.Expression;
import de.vill.model.expression.LengthAggregateFunctionExpression;
import de.vill.model.expression.LiteralExpression;
import de.vill.model.expression.MulExpression;
import de.vill.model.expression.NumberExpression;
import de.vill.model.expression.ParenthesisExpression;
import de.vill.model.expression.StringExpression;
import de.vill.model.expression.SubExpression;

public class UVLConstraintConverter {
	public Result<IExpression> parse(de.vill.model.constraint.Constraint uvlConstraint) {
		List<Literal> dependenciesList = new ArrayList<Literal>();
		
		try {
			IFormula convertedUVLConstraint = (IFormula) parseUVLConstraintRecursively(uvlConstraint, dependenciesList); 
			
			if (!dependenciesList.isEmpty()) {
				And dependenciesAnd = new And(dependenciesList);
				return Result.of(new Implies(dependenciesAnd, convertedUVLConstraint));
			}
			
			return Result.of(convertedUVLConstraint);
		} catch (UVLConstraintConversionException e) {
			return Result.empty(e);
		}
	}
	
	private IExpression parseUVLConstraintRecursively(de.vill.model.constraint.Constraint uvlConstraint, 
			List<Literal> dependenciesList) throws UVLConstraintConversionException {
		if (uvlConstraint instanceof LiteralConstraint) {
			LiteralConstraint literalConstraint = (LiteralConstraint) uvlConstraint;
			VariableReference variableReference = literalConstraint.getReference();
			
		    if (variableReference instanceof de.vill.model.Feature) {
		    	de.vill.model.Feature uvlFeature = (de.vill.model.Feature) variableReference;
		    	return new Literal(uvlFeature.getFeatureName());
		    }
		} else if (uvlConstraint instanceof ParenthesisConstraint) {
			ParenthesisConstraint parenthesisConstraint = (ParenthesisConstraint) uvlConstraint;
			return parseUVLConstraintRecursively(parenthesisConstraint.getContent(), dependenciesList);
		} else if (uvlConstraint instanceof ImplicationConstraint) {
			ImplicationConstraint implicationConstraint = (ImplicationConstraint) uvlConstraint;
			return new Implies((IFormula) parseUVLConstraintRecursively(implicationConstraint.getLeft(), dependenciesList), 
					(IFormula) parseUVLConstraintRecursively(implicationConstraint.getRight(), dependenciesList));
		} else if (uvlConstraint instanceof NotConstraint) {
			NotConstraint notConstraint = (NotConstraint) uvlConstraint;
			return new Not((IFormula) parseUVLConstraintRecursively(notConstraint.getContent(), dependenciesList));
		} else if (uvlConstraint instanceof AndConstraint) {
			AndConstraint andConstraint = (AndConstraint) uvlConstraint;
			return new And((IFormula) parseUVLConstraintRecursively(andConstraint.getLeft(), dependenciesList), 
					(IFormula) parseUVLConstraintRecursively(andConstraint.getRight(), dependenciesList));	
		} else if (uvlConstraint instanceof OrConstraint) {
			OrConstraint orConstraint = (OrConstraint) uvlConstraint;
			return new Or((IFormula) parseUVLConstraintRecursively(orConstraint.getLeft(), dependenciesList), 
					(IFormula) parseUVLConstraintRecursively(orConstraint.getRight(), dependenciesList));	
		} else if (uvlConstraint instanceof MultiOrConstraint) {
			MultiOrConstraint multiOrConstraint = (MultiOrConstraint) uvlConstraint;
			return new Or(getMultiOrAsList(multiOrConstraint.getConstraintSubParts(), dependenciesList));	
		} else if (uvlConstraint instanceof EqualEquationConstraint) {
			EqualEquationConstraint equalConstraint = (EqualEquationConstraint) uvlConstraint;
			return new Equals(parseExpressionConstraint(equalConstraint.getLeft(), dependenciesList), 
					parseExpressionConstraint(equalConstraint.getRight(), dependenciesList));
		} else if (uvlConstraint instanceof EquivalenceConstraint) {
			EquivalenceConstraint equivalenceConstraint = (EquivalenceConstraint) uvlConstraint;
			return new BiImplies((IFormula) parseUVLConstraintRecursively(equivalenceConstraint.getLeft(), dependenciesList), 
					(IFormula) parseUVLConstraintRecursively(equivalenceConstraint.getRight(), dependenciesList));
		} else if (uvlConstraint instanceof LowerEqualsEquationConstraint) {
			LowerEqualsEquationConstraint lowerEqualsConstraint = (LowerEqualsEquationConstraint) uvlConstraint;
			return new LessEqual(parseExpressionConstraint(lowerEqualsConstraint.getLeft(), dependenciesList), 
					parseExpressionConstraint(lowerEqualsConstraint.getRight(), dependenciesList));
		} else if (uvlConstraint instanceof GreaterEqualsEquationConstraint) {
			GreaterEqualsEquationConstraint greaterEqualConstraint = (GreaterEqualsEquationConstraint) uvlConstraint;
			return new GreaterEqual(parseExpressionConstraint(greaterEqualConstraint.getLeft(), dependenciesList), 
					parseExpressionConstraint(greaterEqualConstraint.getRight(), dependenciesList));
		} else if (uvlConstraint instanceof NotEqualsEquationConstraint) {
			NotEqualsEquationConstraint notEqualsConstraint = (NotEqualsEquationConstraint) uvlConstraint;
			return new NotEquals(parseExpressionConstraint(notEqualsConstraint.getLeft(), dependenciesList), 
					parseExpressionConstraint(notEqualsConstraint.getRight(), dependenciesList));
		} else if (uvlConstraint instanceof LowerEquationConstraint) {
			LowerEquationConstraint lowerConstraint = (LowerEquationConstraint) uvlConstraint;
			return new LessThan(parseExpressionConstraint(lowerConstraint.getLeft(), dependenciesList), 
					parseExpressionConstraint(lowerConstraint.getRight(), dependenciesList));
		} else if (uvlConstraint instanceof GreaterEquationConstraint) {
			GreaterEquationConstraint greaterConstraint = (GreaterEquationConstraint) uvlConstraint;
			return new GreaterThan(parseExpressionConstraint(greaterConstraint.getLeft(), dependenciesList), 
					parseExpressionConstraint(greaterConstraint.getRight(), dependenciesList));
		} 
		
		throw new UVLConstraintConversionException(uvlConstraint.getClass().getSimpleName() + " is not supported "
				+ "by the UVLConstraintConverter.");
	}
	
	private ITerm parseExpressionConstraint(Expression expression, List<Literal> dependenciesList) 
			throws UVLConstraintConversionException {
		if (expression instanceof LiteralExpression) {
			LiteralExpression literalExpression = (LiteralExpression) expression;
			VariableReference content = literalExpression.getContent();
			
			if (content instanceof de.vill.model.Feature) {
				de.vill.model.Feature uvlFeature = (de.vill.model.Feature) content;
				Class<?> featureType = getFeatureType(uvlFeature);
				String variableName = uvlFeature.getFeatureName();
				dependenciesList.add(new Literal(variableName + "_def"));
				return new Variable(variableName + "_val", featureType);
			} else if (content instanceof de.vill.model.Attribute) {
				de.vill.model.Attribute uvlAttribute = (de.vill.model.Attribute) content;
		    	return new Constant(uvlAttribute.getValue());
			}
		} else if (expression instanceof ParenthesisExpression) {
			ParenthesisExpression parenthesisExpression = (ParenthesisExpression) expression;
			return parseExpressionConstraint(parenthesisExpression.getContent(), dependenciesList);
		} else if (expression instanceof NumberExpression) {
			NumberExpression numberExpression = (NumberExpression) expression;
			return new Constant(numberExpression.getNumber());
		} else if (expression instanceof StringExpression) {
			StringExpression stringExpression = (StringExpression) expression;
			return new Constant(stringExpression.getString(), String.class);
		}
		else if (expression instanceof AddExpression) {
			AddExpression addExpression = (AddExpression) expression;
			return new IntegerAdd(parseExpressionConstraint(addExpression.getLeft(), dependenciesList), 
					parseExpressionConstraint(addExpression.getRight(), dependenciesList));
		} else if (expression instanceof SubExpression) {
			SubExpression subExpression = (SubExpression) expression;
			return new IntegerAdd(parseExpressionConstraint(subExpression.getLeft(), dependenciesList), 
					new IntegerMultiply(new Constant(-1l), parseExpressionConstraint(subExpression.getRight(), dependenciesList)));
		} else if (expression instanceof MulExpression) {
			MulExpression mulExpression = (MulExpression) expression;
			return new IntegerMultiply(parseExpressionConstraint(mulExpression.getLeft(), dependenciesList), 
					parseExpressionConstraint(mulExpression.getRight(), dependenciesList));
		} else if (expression instanceof DivExpression) {
			DivExpression divExpression = (DivExpression) expression;
			return new IntegerDivide(parseExpressionConstraint(divExpression.getLeft(), dependenciesList), 
					parseExpressionConstraint(divExpression.getRight(), dependenciesList));
		} else if (expression instanceof LengthAggregateFunctionExpression) {
			LengthAggregateFunctionExpression lenghtAggregateExpression = (LengthAggregateFunctionExpression) expression;
			String variableName = lenghtAggregateExpression.getReference().getIdentifier();
			Variable variable = new Variable(variableName + "_val", String.class);
			dependenciesList.add(new Literal(variableName + "_def"));
			return new StringLength(variable);
		}
		
		throw new UVLConstraintConversionException(expression.getClass().getSimpleName() + " is not supported "
				+ "by the UVLConstraintConverter.");
	}
	
	private List<IFormula> getMultiOrAsList(List<de.vill.model.constraint.Constraint> constraints, List<Literal> dependenciesList) 
			throws UVLConstraintConversionException {
		List<IFormula> results = new ArrayList<>();
        for (de.vill.model.constraint.Constraint constraint : constraints) {
        	results.add((IFormula) parseUVLConstraintRecursively(constraint, dependenciesList));
        }
        return results;
	}
	
	private Class<?> getFeatureType(de.vill.model.Feature uvlFeature) throws UVLConstraintConversionException {
		FeatureType type = uvlFeature.getFeatureType();
	    switch (type) {
	        case INT:
	            return Long.class;
	        case REAL:
	            return Double.class;
	        default:
	        	throw new UVLConstraintConversionException("Feature type " + uvlFeature.getFeatureType() + " in ExpressionConstraints is not supported "
	        			+ "by the UVLConstraintConverter.");
	    }
	}
}