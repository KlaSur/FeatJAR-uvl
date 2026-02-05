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

import java.util.List;

import de.featjar.base.data.Result;
import de.featjar.feature.model.Feature;
import de.featjar.feature.model.FeatureModel;
import de.featjar.formula.structure.IExpression;
import de.featjar.formula.structure.IFormula;
import de.featjar.formula.structure.connective.Or;
import de.featjar.formula.structure.predicate.Literal;
import de.vill.model.building.VariableReference;
import de.vill.model.constraint.AndConstraint;

public class UVLConstraintParser {
	public Result<IExpression> parse(FeatureModel featureModel, de.vill.model.constraint.Constraint uvlConstraint) {
		if (uvlConstraint instanceof VariableReference) {
		    Feature feature = new Feature(featureModel);
		    
		    
		} else if (uvlConstraint instanceof AndConstraint) {
			return createAnd(
					parse(uvlConstraint.getLeft(),
					parse(uvlConstraint.getRight());
		}
		
		
		
		
		IFormula formula1 = new Or(new Literal("Test1"), new Literal("Test2"));
		return Result.of(formula1);
	}
	
	public void createAnd(List<de.vill.model.constraint.Constraint> right, 
			List<de.vill.model.constraint.Constraint> left) {
		
	}
}