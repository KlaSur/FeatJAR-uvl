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
package de.featjar.feature.model.io;

import de.featjar.Common;
import de.featjar.FormatTest;
import de.featjar.analysis.javasmt.computation.ComputeJavaSMTFormula;
import de.featjar.analysis.javasmt.computation.ComputeSolutionEnumeration;
import de.featjar.base.FeatJAR;
import de.featjar.base.computation.Computations;
import de.featjar.base.data.Result;
import de.featjar.base.io.format.IFormat;
import de.featjar.base.io.input.FileInputMapper;
import de.featjar.feature.model.io.uvl.UVLFormulaFormat;
import de.featjar.formula.structure.IFormula;
import de.featjar.formula.structure.connective.*;
import de.featjar.formula.structure.predicate.Equals;
import de.featjar.formula.structure.predicate.GreaterEqual;
import de.featjar.formula.structure.predicate.LessThan;
import de.featjar.formula.structure.predicate.Literal;
import de.featjar.formula.structure.term.function.integer.IntegerAdd;
import de.featjar.formula.structure.term.function.integer.IntegerMultiply;
import de.featjar.formula.structure.term.value.Constant;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class UVLFormulaFormatTest extends Common {

    @BeforeAll
    public static void begin() {
        FeatJAR.testConfiguration().initialize();
    }

    @AfterAll
    public static void end() {
        FeatJAR.deinitialize();
    }

    @Test
    void testFixtures() {
        FormatTest.testParse(getFormula("ABC-nAnBnC"), "uvl/ABC-nAnBnC", 1, new UVLFormulaFormat());
        FormatTest.testParse(getFormula("nA"), "uvl/nA", 3, new UVLFormulaFormat());
        FormatTest.testParse(getFormula("nAB"), "uvl/nAB", 1, new UVLFormulaFormat());
        // TODO: testSerializeAndParse
    }

    @Test
    void testUVLFormulaFormatSerialize() throws IOException {
        IFormula formula = new Or(
                new And(new Literal("Test1"), new Literal("Test2")),
                new BiImplies(new Literal("Test3"), new Literal("Test4")),
                new Implies(new Literal("Test5"), new Literal("Test6")),
                new Not(new Literal("Test7")));

        IFormat<IFormula> format = new UVLFormulaFormat();

        Result<String> result = format.serialize(formula);

        if (result.isEmpty()) {
            Assertions.fail();
        }
        String expected = new String(
                Files.readAllBytes(Path.of("src", "test", "resources", "uvl", "formulaSerializeResult.uvl")));
        Assertions.assertEquals(expected, result.get());
    }

    @Test
    void testUVLFormulaFormatParse() throws IOException {
        IFormat<IFormula> format = new UVLFormulaFormat();
        Result<IFormula> result = format.parse(new FileInputMapper(
                Path.of("src", "test", "resources", "uvl", "formulaSerializeResult.uvl"), Charset.defaultCharset()));

        if (result.isEmpty()) {
            Assertions.fail();
        }

        IFormula expected = new Reference(new Or(
                new And(new Literal("Test1"), new Literal("Test2")),
                new Or(
                        new BiImplies(new Literal("Test3"), new Literal("Test4")),
                        new Or(
                                new Implies(new Literal("Test5"), new Literal("Test6")),
                                new Not(new Literal("Test7"))))));

        Assertions.assertEquals(expected, result.get());
    }
    
    @Test
    public void testUVLFormulaFormatParseAndEnumerateSolutions() throws IOException {
    	IFormat<IFormula> format = new UVLFormulaFormat();
        Result<IFormula> computedFormula = format.parse(new FileInputMapper(
                Path.of("src", "test", "resources", "uvl", "MinimalSaladFeatureModel.uvl"), Charset.defaultCharset()));

        if (computedFormula.isEmpty()) {
            Assertions.fail();
        }
        
        final Result<List<List<BooleanFormula>>> computedResult =
                Computations.of((IFormula) computedFormula.get().getChild(0).get())
                .map(ComputeJavaSMTFormula::new)
                .set(ComputeJavaSMTFormula.SOLVER, Solvers.MATHSAT5)
                .map(ComputeSolutionEnumeration::new).computeResult();
        
       IFormula expectedFormula = new And(new Literal("Salad"), new BiImplies(new Literal("Salad"), new Literal("Arugula")),
        		new BiImplies(new Literal("Salad"), new Literal("Veggies")), new Or(new BiImplies(new Literal("Veggies"), new Literal("Tomatoes")),
        		new BiImplies(new Literal("Veggies"), new Literal("Beets")), new BiImplies(new Literal("Veggies"), new Literal("Cucumber")),
        		new BiImplies(new Literal("Veggies"), new Literal("Fennel"))), new Implies(new Literal("Fennel"), new And(new Literal("Beets"), 
        		new Not(new Literal("Cucumber")))), new GreaterEqual(new IntegerAdd(new Constant(90l), new Constant(100l)), new Constant(80d)),
        		new LessThan(new IntegerMultiply(new Constant(80l), new Constant(100l)), new Constant(100000d)),
                new Equals(new Constant(100l), new Constant(100d)));
    	
    	final Result<List<List<BooleanFormula>>> expectedResult =
                Computations.of(expectedFormula)
                .map(ComputeJavaSMTFormula::new)
                .set(ComputeJavaSMTFormula.SOLVER, Solvers.MATHSAT5)
                .map(ComputeSolutionEnumeration::new).computeResult();
    	
    	int size = expectedResult.get().size();
    	Assertions.assertEquals(expectedResult.get().size(), computedResult.get().size());
   
    }
}
