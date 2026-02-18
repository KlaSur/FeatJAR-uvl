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
import java.util.ArrayList;

import de.featjar.base.data.Result;
import de.featjar.feature.model.Feature;
import de.featjar.feature.model.FeatureModel;
import de.featjar.formula.structure.IExpression;
import de.featjar.formula.structure.IFormula;
import de.featjar.formula.structure.connective.BiImplies;
import de.featjar.formula.structure.connective.Implies;
import de.featjar.formula.structure.connective.Not;
import de.featjar.formula.structure.connective.Or;
import de.featjar.formula.structure.predicate.Literal;
import de.vill.model.building.VariableReference;
import de.vill.model.constraint.AndConstraint;
import de.vill.model.constraint.EquivalenceConstraint;
import de.vill.model.constraint.ImplicationConstraint;
import de.vill.model.constraint.LiteralConstraint;
import de.vill.model.constraint.NotConstraint;

public class UVLConstraintParser {
	public Result<IExpression> parse(de.vill.model.constraint.Constraint uvlConstraint) {
		if (uvlConstraint instanceof LiteralConstraint) {
			LiteralConstraint literalConstraint = (LiteralConstraint) uvlConstraint;
			VariableReference variableReference = literalConstraint.getReference();
		    if (variableReference instanceof de.vill.model.Feature) {
		    	de.vill.model.Feature uvlFeature = (de.vill.model.Feature) variableReference;
		    	return Result.of(new Literal(uvlFeature.getFeatureName()));
		    }
		} else if (uvlConstraint instanceof NotConstraint) {
			NotConstraint notConstraint = (NotConstraint) uvlConstraint;
			return Result.of(new Not((IFormula) parse(notConstraint.getContent()).get()));
		} else if (uvlConstraint instanceof ImplicationConstraint) {
			ImplicationConstraint implicationConstraint = (ImplicationConstraint) uvlConstraint;
			return Result.of(new Implies((IFormula) parse(implicationConstraint.getLeft()).get(), 
					(IFormula) parse(implicationConstraint.getRight()).get()));
		} else if (uvlConstraint instanceof EquivalenceConstraint) {
			EquivalenceConstraint equivalenceConstraint = (EquivalenceConstraint) uvlConstraint;
			return Result.of(new BiImplies((IFormula) parse(equivalenceConstraint.getLeft()).get(), 
					(IFormula) parse(equivalenceConstraint.getRight()).get()));
		} else {
            return Result.empty(new ArrayList<>());
        }
		
		return Result.empty(new ArrayList<>());
	}
	
}