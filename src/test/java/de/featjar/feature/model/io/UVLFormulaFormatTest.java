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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.visitors.BooleanFormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.DefaultBooleanFormulaVisitor;

import de.featjar.Common;
import de.featjar.FormatTest;
import de.featjar.analysis.javasmt.computation.ComputeJavaSMTFormula;
import de.featjar.analysis.javasmt.computation.ComputeSolutionEnumeration;
import de.featjar.analysis.javasmt.solver.FormulaToJavaSMT;
import de.featjar.analysis.javasmt.solver.FormulaToJavaSMT.VariableReference;
import de.featjar.analysis.javasmt.solver.JavaSMTFormula;
import de.featjar.base.FeatJAR;
import de.featjar.base.computation.Computations;
import de.featjar.base.data.Result;
import de.featjar.base.io.format.IFormat;
import de.featjar.base.io.input.FileInputMapper;
import de.featjar.feature.model.io.uvl.UVLFormulaFormat;
import de.featjar.formula.VariableMap;
import de.featjar.formula.assignment.BooleanAssignment;
import de.featjar.formula.structure.IFormula;
import de.featjar.formula.structure.connective.And;
import de.featjar.formula.structure.connective.BiImplies;
import de.featjar.formula.structure.connective.Implies;
import de.featjar.formula.structure.connective.Not;
import de.featjar.formula.structure.connective.Or;
import de.featjar.formula.structure.connective.Reference;
import de.featjar.formula.structure.predicate.Equals;
import de.featjar.formula.structure.predicate.GreaterEqual;
import de.featjar.formula.structure.predicate.GreaterThan;
import de.featjar.formula.structure.predicate.LessEqual;
import de.featjar.formula.structure.predicate.LessThan;
import de.featjar.formula.structure.predicate.Literal;
import de.featjar.formula.structure.term.function.integer.IntegerAdd;
import de.featjar.formula.structure.term.function.integer.IntegerDivide;
import de.featjar.formula.structure.term.function.integer.IntegerMultiply;
import de.featjar.formula.structure.term.function.string.StringLength;
import de.featjar.formula.structure.term.value.Constant;
import de.featjar.formula.structure.term.value.Variable;

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
    public void testUVLFormulaFormatParseWithMinimalSaladFeatureModel() throws IOException {
    	IFormat<IFormula> format = new UVLFormulaFormat();
        Result<IFormula> computedFormula = format.parse(new FileInputMapper(
                Path.of("src", "test", "resources", "uvl", "MinimalSaladFeatureModel.uvl"), Charset.defaultCharset()));

        if (computedFormula.isEmpty()) {
            Assertions.fail();
        }
        
        IFormula expectedFormula = new Reference(new And(new And(new And(new Literal("Veggies"), new Or(new Literal("Tomatoes"), new Literal("Cucumber"),
    		    new Literal("Fennel"), new Literal("Beets")))), 
    		    new Implies(new Literal("Fennel"), new And(new Literal("Beets"), new Not(new Literal("Cucumber")))), 
    		    new GreaterEqual(new IntegerAdd(new Constant(90l), new Constant(100l)), new Constant(80d)),
        		new LessThan(new IntegerMultiply(new Constant(80l), new Constant(100l)), new Constant(100000d)),
                new Equals(new Constant(100l), new Constant(100d)), 
                new BiImplies(new Literal("Beets"), new Or(new Literal("Cucumber"), new Not(new Literal("Tomatoes")))),
                new LessEqual(new IntegerAdd(new Constant(100l), new IntegerMultiply(new Constant(-1l), new Constant(80l))), new Constant(30d)),
                new GreaterThan(new IntegerDivide(new Constant(100l), new Constant(25l)), new Constant(3d)), 
                new Equals(new Constant("Cherry"), new Constant("Cherry")),
                new Implies(new And(new Literal("Arugula_def")), 
                new Equals(new StringLength(new Variable("Arugula_val", String.class)), new Constant(7d)))));
       
       Assertions.assertEquals(computedFormula.get(), expectedFormula);
   }
    
   @Test
   public void testEnumerateSolutionsWithMinimalSaladFeatureModel() {
	   IFormula expectedFormula = new Reference(new And(new And(new And(new Literal("Veggies"), new Or(new Literal("Tomatoes"), new Literal("Cucumber"),
   		    new Literal("Fennel"), new Literal("Beets")))), 
   		    new Implies(new Literal("Fennel"), new And(new Literal("Beets"), new Not(new Literal("Cucumber")))), 
   		    new GreaterEqual(new IntegerAdd(new Constant(90l), new Constant(100l)), new Constant(80d)),
       		new LessThan(new IntegerMultiply(new Constant(80l), new Constant(100l)), new Constant(100000d)),
            new Equals(new Constant(100l), new Constant(100d)), 
            new BiImplies(new Literal("Beets"), new Or(new Literal("Cucumber"), new Not(new Literal("Tomatoes")))),
            new LessEqual(new IntegerAdd(new Constant(100l), new IntegerMultiply(new Constant(-1l), new Constant(80l))), new Constant(30d)),
            new GreaterThan(new IntegerDivide(new Constant(100l), new Constant(25l)), new Constant(3d)), 
            new Equals(new Constant("Cherry"), new Constant("Cherry")),
            new Implies(new And(new Literal("Arugula_def")), 
            new Equals(new StringLength(new Variable("Arugula_val", String.class)), new Constant(7d)))));
	   
	   final Result<JavaSMTFormula> javaSMTFormulaResult =
               Computations.of(expectedFormula)
               .map(ComputeJavaSMTFormula::new)
               .set(ComputeJavaSMTFormula.SOLVER, Solvers.Z3)
               .computeResult();
	   
	   JavaSMTFormula javaSMTFormula = javaSMTFormulaResult.get();
	   FormulaToJavaSMT formulaToJavaSmt = javaSMTFormula.getTranslator();
	   VariableMap variableMap = javaSMTFormula.getVariableMap();
	   
	   formulaToJavaSmt.nodeToFormula(expectedFormula);
	   FormulaManager formulaManager = formulaToJavaSmt.getCurrentFormulaManager();
	   List<VariableReference> variableReferences = formulaToJavaSmt.getMappings();
	   
	   Result<List<List<BooleanFormula>>> booleanAssignmentsResult = Computations.of(javaSMTFormula)
			   .map(ComputeSolutionEnumeration::new)
			   .computeResult();
	   
	   List<List<BooleanFormula>> booleanAssignments = booleanAssignmentsResult.get();
	   
	   
	   
	   // define a BooleanFormulaVisitor which returns the index of the current atom
       BooleanFormulaVisitor<Integer> booleanFormulaVisitor = (BooleanFormulaVisitor<Integer>) new DefaultBooleanFormulaVisitor<Integer>() {
		   public Integer visitNot(BooleanFormula operand) {
			   Variable variable = variableReferences.stream().filter(r -> r.getJavaSmtVariable().equals(operand))
					   .map(r -> r.getVariable()).findFirst().orElse(null);
			   
			   Integer index = variableMap.get(variable.getName()).get();
			  
			   return -index;
		   }
		   
		   public Integer visitAtom(BooleanFormula atom, FunctionDeclaration<BooleanFormula> funcDecl) {
			   Variable variable = variableReferences.stream().filter(r -> r.getJavaSmtVariable().equals(atom))
					   .map(r -> r.getVariable()).findFirst().orElse(null);
			   
			   Integer index = variableMap.get(variable.getName()).get();
			   
			   return index;
		   }

		   public Integer visitDefault() {
			   return -1000;
		   }
	    };
	    
	    
	    
	    // create computed assignments
	    List<BooleanAssignment> computedAssignments = new ArrayList<BooleanAssignment>();
	    for (List<BooleanFormula> booleanAssignment : booleanAssignments) {
	    	BooleanAssignment satisfyingAssignment = new BooleanAssignment();
	    	for (BooleanFormula booleanVariable : booleanAssignment) {
	    		Integer assignment = formulaManager.getBooleanFormulaManager()
	    				.visit(booleanVariable, booleanFormulaVisitor);
	    		satisfyingAssignment = satisfyingAssignment.addAll(assignment.intValue());
	    	}
	    	computedAssignments.add(satisfyingAssignment);
	    }
	
	   
	   // create expected assignments
	   List<BooleanAssignment> expectedAssignments = new ArrayList<BooleanAssignment>();
	   BooleanAssignment satisfyingAssignment1 = new BooleanAssignment(1, 2, -3, -4, -5, -6);
	   BooleanAssignment satisfyingAssignment2 = new BooleanAssignment(1, -2, -4, 5, -6);
	   BooleanAssignment satisfyingAssignment3 = new BooleanAssignment(1, -2, -3, 4, 5, -6);
	   BooleanAssignment satisfyingAssignment4 = new BooleanAssignment(1, 2, 3, -4, 5, -6);
	   BooleanAssignment satisfyingAssignment5 = new BooleanAssignment(1, 2, -3, -4, -5, 6);
	   BooleanAssignment satisfyingAssignment6 = new BooleanAssignment(1, -2, -4, 5, 6);
	   BooleanAssignment satisfyingAssignment7 = new BooleanAssignment(1, 2, 3, -4, 5, 6);
	   BooleanAssignment satisfyingAssignment8 = new BooleanAssignment(1, -2, -3, 4, 5, 6);
	   
	   expectedAssignments.add(satisfyingAssignment1);
	   expectedAssignments.add(satisfyingAssignment2);
	   expectedAssignments.add(satisfyingAssignment3);
	   expectedAssignments.add(satisfyingAssignment4);
	   expectedAssignments.add(satisfyingAssignment5);
	   expectedAssignments.add(satisfyingAssignment6);
	   expectedAssignments.add(satisfyingAssignment7);
	   expectedAssignments.add(satisfyingAssignment8);
	  
	   
	   Assertions.assertEquals(computedAssignments, expectedAssignments); 
   }
	   
}
