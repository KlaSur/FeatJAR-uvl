/*
 * Copyright (C) 2026 FeatJAR-Development-Team
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
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.featjar.FormatTest;
import de.featjar.analysis.sat4j.computation.ComputeSatisfiableSAT4J;
import de.featjar.base.computation.Computations;
import de.featjar.base.data.Attribute;
import de.featjar.base.data.Attributes;
import de.featjar.base.data.Name;
import de.featjar.base.data.Result;
import de.featjar.base.data.identifier.Identifiers;
import de.featjar.base.io.format.IFormat;
import de.featjar.base.io.input.FileInputMapper;
import de.featjar.feature.model.FeatureModel;
import de.featjar.feature.model.IConstraint;
import de.featjar.feature.model.IFeature;
import de.featjar.feature.model.IFeatureModel;
import de.featjar.feature.model.IFeatureTree;
import de.featjar.feature.model.io.uvl.UVLFeatureModelFormat;
import de.featjar.formula.assignment.conversion.ComputeBooleanClauseList;
import de.featjar.formula.computation.ComputeCNFFormula;
import de.featjar.formula.computation.ComputeNNFFormula;
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
import de.featjar.formula.structure.term.function.integer.IntegerAdd;
import de.featjar.formula.structure.term.function.integer.IntegerDivide;
import de.featjar.formula.structure.term.function.integer.IntegerMultiply;
import de.featjar.formula.structure.term.function.string.StringLength;
import de.featjar.formula.structure.term.value.Constant;
import de.featjar.formula.structure.term.value.Variable;

public class UVLFeatureModelFormatTest {

    private static FeatureModel featureModel;

    @BeforeAll
    public static void setup() {
        FeatureModel featureModel = new FeatureModel(Identifiers.newCounterIdentifier());

        // features
        IFeatureTree rootTree =
                featureModel.mutate().addFeatureTreeRoot(featureModel.mutate().addFeature("root"));
        rootTree.mutate().toAndGroup();

        IFeature childFeature1 = featureModel.mutate().addFeature("Test1");
        IFeatureTree childTree1 = rootTree.mutate().addFeatureBelow(childFeature1);
        childTree1.mutate().toAlternativeGroup();

        IFeature childFeature2 = featureModel.mutate().addFeature("Test2");
        IFeatureTree childTree2 = rootTree.mutate().addFeatureBelow(childFeature2);
        childTree2.mutate().toOrGroup();

        IFeature childFeature3 = featureModel.mutate().addFeature("Test3");
        childTree1.mutate().addFeatureBelow(childFeature3);

        IFeature childFeature4 = featureModel.mutate().addFeature("Test4");
        childTree1.mutate().addFeatureBelow(childFeature4);

        IFeature childFeature5 = featureModel.mutate().addFeature("Test5");
        childTree2.mutate().addFeatureBelow(childFeature5);

        IFeature childFeature6 = featureModel.mutate().addFeature("Test6");
        childTree2.mutate().addFeatureBelow(childFeature6);

        IFeature childFeature7 = featureModel.mutate().addFeature("Test7");
        IFeatureTree childTree7 = rootTree.mutate().addFeatureBelow(childFeature7);
        childTree7.mutate().makeMandatory();

        IFormula formula1 = new Or(
                new And(new Literal("Test1"), new Literal("Test2")),
                new BiImplies(new Literal("Test3"), new Literal("Test4")),
                new Implies(new Literal("Test5"), new Literal("Test6")),
                new Not(new Literal("Test7")));

        // constraints
        featureModel.mutate().addConstraint(formula1);
        UVLFeatureModelFormatTest.featureModel = featureModel;
    }

    @Test
    void testFixtures() {
        FormatTest.testParseAndSerialize("uvl/ABC-nAnBnC", new UVLFeatureModelFormat());
        FormatTest.testParseAndSerialize("uvl/nA", new UVLFeatureModelFormat());
        FormatTest.testParseAndSerialize("uvl/nAB", new UVLFeatureModelFormat());
    }

    @Test
    void testUVLFeatureModelFormatSerialize() throws IOException {
        UVLFeatureModelFormat format = new UVLFeatureModelFormat();
        Result<String> featureModelString = format.serialize(featureModel);

        if (featureModelString.isEmpty()) {
            Assertions.fail();
        }

        String expected = new String(
                Files.readAllBytes(Path.of("src", "test", "resources", "uvl", "featureModelSerializeResult.uvl")));
        Assertions.assertEquals(expected, featureModelString.get());
    }

    @Test
    void testUVLFeatureModelFormatParse() throws IOException {
        IFormat<IFeatureModel> format = new UVLFeatureModelFormat();
        Result<IFeatureModel> result = format.parse(new FileInputMapper(
                Path.of("src", "test", "resources", "uvl", "featureModelSerializeResult.uvl"),
                Charset.defaultCharset()));

        if (result.isEmpty()) {
            Assertions.fail();
        }

        IFeatureModel parsedFeatureModel = result.get();

        // testing root
        IFeature rootFeature = parsedFeatureModel.getFeature("root").get();
        List<String> rootChildrenNames = rootFeature.getFeatureTree().get().getChildren().stream()
                .map((it) -> it.getFeature().getName().get())
                .collect(Collectors.toList());
        Assertions.assertEquals(3, rootChildrenNames.size());
        Assertions.assertTrue(rootChildrenNames.contains("Test1"));
        Assertions.assertTrue(rootChildrenNames.contains("Test2"));
        Assertions.assertTrue(rootChildrenNames.contains("Test7"));

        // testing Test1 feature
        IFeature test1Feature = parsedFeatureModel.getFeature("Test1").get();
        Assertions.assertTrue(
                test1Feature.getFeatureTree().get().getParentGroup().get().isAnd());
        Assertions.assertTrue(test1Feature.getFeatureTree().get().isOptional());
        List<String> test1ChildrenNames = test1Feature.getFeatureTree().get().getChildren().stream()
                .map((it) -> it.getFeature().getName().get())
                .collect(Collectors.toList());
        Assertions.assertEquals(2, test1ChildrenNames.size());
        Assertions.assertTrue(test1ChildrenNames.contains("Test3"));
        Assertions.assertTrue(test1ChildrenNames.contains("Test4"));

        // testing Test2 feature
        IFeature test2Feature = parsedFeatureModel.getFeature("Test2").get();
        Assertions.assertTrue(
                test2Feature.getFeatureTree().get().getParentGroup().get().isAnd());
        Assertions.assertTrue(test2Feature.getFeatureTree().get().isOptional());
        List<String> test2ChildrenNames = test2Feature.getFeatureTree().get().getChildren().stream()
                .map((it) -> it.getFeature().getName().get())
                .collect(Collectors.toList());
        Assertions.assertEquals(2, test2ChildrenNames.size());
        Assertions.assertTrue(test2ChildrenNames.contains("Test5"));
        Assertions.assertTrue(test2ChildrenNames.contains("Test6"));

        // testing Test3 feature
        IFeature test3Feature = parsedFeatureModel.getFeature("Test3").get();
        Assertions.assertTrue(
                test3Feature.getFeatureTree().get().getParentGroup().get().isAlternative());
        Assertions.assertTrue(test3Feature.getFeatureTree().get().getChildren().isEmpty());

        // testing Test4 feature
        IFeature test4Feature = parsedFeatureModel.getFeature("Test4").get();
        Assertions.assertTrue(
                test4Feature.getFeatureTree().get().getParentGroup().get().isAlternative());
        Assertions.assertTrue(test4Feature.getFeatureTree().get().getChildren().isEmpty());

        // testing Test5 feature
        IFeature test5Feature = parsedFeatureModel.getFeature("Test5").get();
        Assertions.assertTrue(
                test5Feature.getFeatureTree().get().getParentGroup().get().isOr());
        Assertions.assertTrue(test5Feature.getFeatureTree().get().getChildren().isEmpty());

        // testing Test6 feature
        IFeature test6Feature = parsedFeatureModel.getFeature("Test6").get();
        Assertions.assertTrue(
                test6Feature.getFeatureTree().get().getParentGroup().get().isOr());
        Assertions.assertTrue(test6Feature.getFeatureTree().get().getChildren().isEmpty());

        // testing Test7 feature
        IFeature test7Feature = parsedFeatureModel.getFeature("Test7").get();
        Assertions.assertTrue(
                test7Feature.getFeatureTree().get().getParentGroup().get().isAnd());
        Assertions.assertTrue(test7Feature.getFeatureTree().get().isMandatory());
        Assertions.assertTrue(test7Feature.getFeatureTree().get().getChildren().isEmpty());

        Assertions.assertEquals(1, parsedFeatureModel.getConstraints().size());
        IFormula constraint =
                parsedFeatureModel.getConstraints().iterator().next().getFormula();
        IFormula constraint2 = featureModel.getConstraints().iterator().next().getFormula();

        Boolean notEquivalent = Computations.of((IFormula) new Not(new BiImplies(constraint, constraint2)))
                .map(ComputeNNFFormula::new)
                .map(ComputeCNFFormula::new)
                .map(ComputeBooleanClauseList::new)
                .map(ComputeSatisfiableSAT4J::new)
                .compute();

        Assertions.assertFalse(notEquivalent);
    }
    
    @Test
    void testSaladFeatureModel1() throws IOException {
    	IFormat<IFeatureModel> format = new UVLFeatureModelFormat();
        Result<IFeatureModel> result = format.parse(new FileInputMapper(
                Path.of("src", "test", "resources", "uvl", "SaladFeatureModel.uvl"),
                Charset.defaultCharset()));

        if (result.isEmpty()) {
            Assertions.fail();
        }

        IFeatureModel parsedFeatureModel = result.get();
        List<IConstraint> constraints = new ArrayList<>(parsedFeatureModel.getConstraints());
        
        IFormula impliesConstraint = new Implies(new Literal("Fennel"), new And(new Literal("Beets"), new Not(new Literal("Cucumber"))));
        IFormula greaterEqualConstraint = new GreaterEqual(new IntegerAdd(new Constant(90l), new Constant(100l)), new Constant(80d));
        IFormula lowerConstraint = new LessThan(new IntegerMultiply(new Constant(80l), new Constant(90l)), new Constant(1600d));
        IFormula equalConstraint = new Equals(new Constant(100l), new Constant(110d));
        
        Assertions.assertEquals(constraints.get(0).getFormula().getChild(0).get(), impliesConstraint);
        Assertions.assertEquals(constraints.get(1).getFormula().getChild(0).get(), greaterEqualConstraint);
        Assertions.assertEquals(constraints.get(2).getFormula().getChild(0).get(), lowerConstraint);
        Assertions.assertEquals(constraints.get(3).getFormula().getChild(0).get(), equalConstraint);
    }
        
    @Test
    void testSaladFeatureModel2() throws IOException {
    	IFormat<IFeatureModel> format = new UVLFeatureModelFormat();
        Result<IFeatureModel> result = format.parse(new FileInputMapper(
                Path.of("src", "test", "resources", "uvl", "SaladFeatureModel2.uvl"),
                Charset.defaultCharset()));

        if (result.isEmpty()) {
            Assertions.fail();
        }

        IFeatureModel parsedFeatureModel = result.get();
        List<IConstraint> constraints = new ArrayList<>(parsedFeatureModel.getConstraints());
        
        IFormula biImpliesConstraint = new BiImplies(new Literal("Beans"), new Or(new Literal("Shallots"), new Not(new Literal("Tomatoes"))));
        IFormula lessEqualConstraint = new LessEqual(new IntegerAdd(new Constant(90l), new IntegerMultiply(new Constant(-1l), new Constant(80l))),
        		new Constant(20d));
        IFormula greaterThanConstraint = new GreaterThan(new IntegerDivide(new Constant(100l), new Constant(25l)), new Constant(3d));
        IFormula notEqualsConstraint = new NotEquals(new Constant("Cherry", String.class), new Constant("Roma", String.class));
        IFormula stringLengthConstraint = new Equals(new StringLength(new Variable("Beans", String.class)), new Constant(6d));
        
        Assertions.assertEquals(constraints.get(0).getFormula().getChild(0).get(), biImpliesConstraint);
        Assertions.assertEquals(constraints.get(1).getFormula().getChild(0).get(), lessEqualConstraint);
        Assertions.assertEquals(constraints.get(2).getFormula().getChild(0).get(), greaterThanConstraint);
        Assertions.assertEquals(constraints.get(3).getFormula().getChild(0).get(), notEqualsConstraint);
        Assertions.assertEquals(constraints.get(4).getFormula().getChild(0).get(), stringLengthConstraint);
    }
    
    @Test
    void testUVLMinimalSaladFeatureModelFormatParse() throws IOException {
    	IFormat<IFeatureModel> format = new UVLFeatureModelFormat();
        Result<IFeatureModel> result = format.parse(new FileInputMapper(
                Path.of("src", "test", "resources", "uvl", "MinimalSaladFeatureModel.uvl"),
                Charset.defaultCharset()));

        if (result.isEmpty()) {
            Assertions.fail();
        }
        
        IFeatureModel parsedFeatureModel = result.get();
    	
//    	IFeatureTree saladRoot =
//                featureModel.mutate().addFeatureTreeRoot(featureModel.mutate().addFeature("Salad"));
//        saladRoot.mutate().toAndGroup();
//
//        // mandatory Veggies
//        IFeature mandatoryVeggies = featureModel.mutate().addFeature("Veggies");
//        IFeatureTree mandatoryVeggiesTree = saladRoot.mutate().addFeatureBelow(mandatoryVeggies);
//        mandatoryVeggiesTree.mutate().makeMandatory();
//        mandatoryVeggiesTree.mutate().toOrGroup();
//        
//        // optional String Arugula {Freshness 90, FoodMiles 20}
//        IFeature optionalArugula = featureModel.mutate().addFeature("Arugula");
//        optionalArugula.mutate().setType(String.class);
//        IFeatureTree optionalArugulaTree = saladRoot.mutate().addFeatureBelow(optionalArugula);
//        optionalArugulaTree.mutate().makeOptional();
//        
//        
//        // Tomatoes {Freshness 30, FoodMiles 100, tomatoType 'Cherry'}
//        IFeature tomatoes = featureModel.mutate().addFeature("Tomatoes");
//        IFeatureTree tomatoesTree = mandatoryVeggiesTree.mutate().addFeatureBelow(tomatoes);
//        
//        Attribute<Integer> tomatoesFreshness = Attributes.get(new Name("Freshness"), Integer.class);
//        tomatoesTree.mutate().setAttributeValue(tomatoesFreshness, 30);
//        
//        Attribute<Integer> tomatoesFoodMiles = Attributes.get(new Name("FoodMiles"), Integer.class);
//        tomatoesTree.mutate().setAttributeValue(tomatoesFoodMiles, 100);
//        
//        Attribute<String> tomatoesType = Attributes.get(new Name("tomatoType"), String.class);
//        tomatoesTree.mutate().setAttributeValue(tomatoesType, "Cherry");
//        
//        
//        // Cucumber {Freshness 25, FoodMiles 80}
//        IFeature cucumber = featureModel.mutate().addFeature("Cucumber");
//        IFeatureTree cucumberTree = mandatoryVeggiesTree.mutate().addFeatureBelow(cucumber);
//        
//        Attribute<Integer> cucumberFreshness = Attributes.get(new Name("Freshness"), Integer.class);
//        cucumberTree.mutate().setAttributeValue(cucumberFreshness, 25);
//        
//        Attribute<Integer> cucumberFoodMiles = Attributes.get(new Name("FoodMiles"), Integer.class);
//        cucumberTree.mutate().setAttributeValue(cucumberFoodMiles, 80);
//        
//        
//        // Fennel {Freshness 100, FoodMiles 11}
//        IFeature fennel = featureModel.mutate().addFeature("Fennel");
//        IFeatureTree fennelTree = mandatoryVeggiesTree.mutate().addFeatureBelow(fennel);
//        
//        Attribute<Integer> fennelFreshness = Attributes.get(new Name("Freshness"), Integer.class);
//        fennelTree.mutate().setAttributeValue(fennelFreshness, 100);
//        
//        Attribute<Integer> fennelFoodMiles = Attributes.get(new Name("FoodMiles"), Integer.class);
//        fennelTree.mutate().setAttributeValue(fennelFoodMiles, 11);
//        
//        
//        // Beets {Freshness 150, FoodMiles 5}
//        IFeature beets = featureModel.mutate().addFeature("Beets");
//        IFeatureTree beetsTree = mandatoryVeggiesTree.mutate().addFeatureBelow(beets);
//        
//        Attribute<Integer> beetsFreshness = Attributes.get(new Name("Freshness"), Integer.class);
//        beetsTree.mutate().setAttributeValue(beetsFreshness, 150);
//        
//        Attribute<Integer> beetsFoodMiles = Attributes.get(new Name("FoodMiles"), Integer.class);
//        beetsTree.mutate().setAttributeValue(beetsFoodMiles, 5);
//        
        
        // testing Salad feature
        IFeature rootFeature = parsedFeatureModel.getFeature("Salad").get();
        List<String> rootChildrenNames = rootFeature.getFeatureTree().get().getChildren().stream()
                .map((it) -> it.getFeature().getName().get())
                .collect(Collectors.toList());
        Assertions.assertEquals(2, rootChildrenNames.size());
        Assertions.assertTrue(rootChildrenNames.contains("Veggies"));
        Assertions.assertTrue(rootChildrenNames.contains("Arugula"));
        
        // testing Veggies feature
        IFeature mandatoryVeggies = parsedFeatureModel.getFeature("Veggies").get();
        Assertions.assertTrue(
        		mandatoryVeggies.getFeatureTree().get().getParentGroup().get().isOr());
        Assertions.assertTrue(mandatoryVeggies.getFeatureTree().get().isMandatory());
        List<String> mandatoryVeggiesChildren = mandatoryVeggies.getFeatureTree().get().getChildren().stream()
                .map((it) -> it.getFeature().getName().get())
                .collect(Collectors.toList());
        Assertions.assertEquals(4, mandatoryVeggiesChildren.size());
        Assertions.assertTrue(mandatoryVeggiesChildren.contains("Tomatoes"));
        Assertions.assertTrue(mandatoryVeggiesChildren.contains("Cucumber"));
        Assertions.assertTrue(mandatoryVeggiesChildren.contains("Fennel"));
        Assertions.assertTrue(mandatoryVeggiesChildren.contains("Beets"));
        
        // testing Arugula feature
        IFeature optionalArugula = parsedFeatureModel.getFeature("Arugula").get();
        Assertions.assertTrue(optionalArugula.getFeatureTree().get().isOptional());
        List<String> optionalArugulaChildren = optionalArugula.getFeatureTree().get().getChildren().stream()
                .map((it) -> it.getFeature().getName().get())
                .collect(Collectors.toList());
        Assertions.assertEquals(0, optionalArugulaChildren.size());
       
        
        
        
        
     
        
    }
}
