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
package de.featjar.feature.model.io.uvl;

import de.featjar.base.data.Attribute;
import de.featjar.base.data.Attributes;
import de.featjar.base.data.Name;
import de.featjar.base.data.Problem;
import de.featjar.base.data.Range;
import de.featjar.base.data.Result;
import de.featjar.base.data.identifier.Identifiers;
import de.featjar.base.io.IO;
import de.featjar.base.io.format.IFormat;
import de.featjar.base.io.input.AInputMapper;
import de.featjar.base.io.input.FileInputMapper;
import de.featjar.base.tree.Trees;
import de.featjar.feature.model.*;
import de.featjar.feature.model.FeatureTree.Group;
import de.featjar.feature.model.io.uvl.visitor.FeatureTreeToUVLFeatureModelVisitor;
import de.featjar.feature.model.io.uvl.visitor.FormulaToUVLConstraintVisitor;
import de.featjar.formula.structure.IFormula;
import de.featjar.formula.structure.connective.And;
import de.featjar.formula.structure.connective.BiImplies;
import de.featjar.formula.structure.connective.Implies;
import de.featjar.formula.structure.connective.Not;
import de.featjar.formula.structure.connective.Or;
import de.featjar.formula.structure.predicate.LessThan;
import de.featjar.formula.structure.predicate.Literal;
import de.vill.main.UVLModelFactory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;

/**
 * Parses and writes feature models from and to UVL files.
 *
 * @author Sebastian Krieter
 * @author Andreas Gerasimow
 */
public class UVLFeatureModelFormat implements IFormat<IFeatureModel> {

    @Override
    public Result<IFeatureModel> parse(AInputMapper inputMapper) {
        try {
            String content = inputMapper.get().text();
            UVLModelFactory uvlModelFactory = new UVLModelFactory();
            de.vill.model.FeatureModel uvlModel = uvlModelFactory.parse(content);

            IFeatureModel featureModel = UVLFeatureModelToFeatureTree.createFeatureModel(uvlModel);
            List<IFormula> formulas = UVLFeatureModelToFeatureTree.uvlConstraintToFormula(uvlModel.getConstraints());
            formulas.forEach((formula) -> featureModel.mutate().addConstraint(formula));

             return Result.of(featureModel);
        } catch (Exception e) {
            return Result.empty(e);
        }
    }

    @Override
    public Result<String> serialize(IFeatureModel fm) {
        List<Problem> problems = new ArrayList<>();
        try {
            if (fm.getRootFeatures().isEmpty()) {
                problems.add(new Problem("No root features exists.", Problem.Severity.ERROR));
                return Result.empty(problems);
            }

            IFeature rootFeature = fm.getRootFeatures().get(0);
            problems.add(new Problem(
                    "UVL supports only one root feature. If there are more than one root features in the model, the first one will be used.",
                    Problem.Severity.WARNING));

            Result<IFeatureTree> featureTree = fm.getFeatureTree(rootFeature);
            problems.addAll(featureTree.getProblems());
            if (featureTree.isEmpty()) {
                return Result.empty(problems);
            }
            Result<de.vill.model.FeatureModel> uvlModel =
                    Trees.traverse(featureTree.get(), new FeatureTreeToUVLFeatureModelVisitor());
            problems.addAll(uvlModel.getProblems());
            if (uvlModel.isEmpty()) {
                return Result.empty(problems);
            }

            for (IConstraint constraint : fm.getConstraints()) {
                Result<de.vill.model.constraint.Constraint> uvlConstraint =
                        Trees.traverse(constraint.getFormula(), new FormulaToUVLConstraintVisitor());
                problems.addAll(uvlConstraint.getProblems());
                if (uvlConstraint.isEmpty()) {
                    return Result.empty(problems);
                }
                uvlModel.get().getOwnConstraints().add(uvlConstraint.get());
            }

            String test = uvlModel.get().toString();
            return Result.of(uvlModel.get().toString(), problems);
        } catch (Exception e) {
            return Result.empty(e);
        }
    }

    @Override
    public boolean supportsParse() {
        return true;
    }

    @Override
    public boolean supportsWrite() {
        return true;
    }

    @Override
    public String getFileExtension() {
        return "uvl";
    }

    @Override
    public String getName() {
        return "Universal Variability Language";
    }
    
    public static <T> void testParseAndSerialize(String name, IFormat<T> format) {
        assertEquals(format.getClass().getCanonicalName(), format.getIdentifier());
        assertTrue(format.supportsParse());
        assertTrue(format.supportsWrite());

        // parse
        final byte[][] byteArrays = getByteArrays(name, 1, format);
        assertEquals(1, byteArrays.length);
        final byte[] parseInput = byteArrays[0];
        final Result<T> result = IO.load(new ByteArrayInputStream(parseInput), format, StandardCharsets.UTF_8);
        assertNotNull(result);
        T obj = result.get();

        // serialize
        final byte[] serializeOutput = serialize(obj, format);

        assertArrayEquals(parseInput, serializeOutput);
    }
    
    private static <T> byte[] serialize(T object, IFormat<T> format) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            IO.save(object, out, format);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

    private static <T> byte[][] getByteArrays(String name, int count, IFormat<T> format) {
        byte[][] result = new byte[count][];
        for (int i = 1; i <= count; i++) {
            // URL systemResource = ClassLoader.getSystemResource(
            // String.format("formats/%s_%02d.%s", name, i, format.getFileExtension()));
        	
        	URL systemResource = ClassLoader.getSystemResource(
                String.format("%s.%s", name, format.getFileExtension()));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream stream = systemResource.openStream()) {
                byte[] buffer = new byte[1024];
                int n;
                while ((n = stream.read(buffer)) != -1) {
                    baos.write(buffer, 0, n);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Assertions.fail();
            }
            result[i - 1] = baos.toByteArray();
        }
        return result;
    }
    
    public static void testParseWithGroupCardinality() throws IOException {
    	FeatureModel featureModelGroupCardinalities = new FeatureModel(Identifiers.newCounterIdentifier());

        // features
        IFeatureTree rootTree =
        		featureModelGroupCardinalities.mutate().addFeatureTreeRoot(
        				featureModelGroupCardinalities.mutate().addFeature("root"));
        rootTree.mutate().toAndGroup();

        IFeature childFeature1 = featureModelGroupCardinalities.mutate().addFeature("Test1");
        IFeatureTree childTree1 = rootTree.mutate().addFeatureBelow(childFeature1);
        childTree1.mutate().toAlternativeGroup();

        IFeature childFeature2 = featureModelGroupCardinalities.mutate().addFeature("Test2");
        IFeatureTree childTree2 = rootTree.mutate().addFeatureBelow(childFeature2);
        childTree2.mutate().toOrGroup();

        IFeature childFeature3 = featureModelGroupCardinalities.mutate().addFeature("Test3");
        childTree1.mutate().addFeatureBelow(childFeature3);

        IFeature childFeature4 = featureModelGroupCardinalities.mutate().addFeature("Test4");
        childTree1.mutate().addFeatureBelow(childFeature4);

        IFeature childFeature5 = featureModelGroupCardinalities.mutate().addFeature("Test5");
        childTree2.mutate().addFeatureBelow(childFeature5);

        IFeature childFeature6 = featureModelGroupCardinalities.mutate().addFeature("Test6");
        childTree2.mutate().addFeatureBelow(childFeature6);

        IFeature childFeature7 = featureModelGroupCardinalities.mutate().addFeature("Test7");
        IFeatureTree childTree7 = rootTree.mutate().addFeatureBelow(childFeature7);
        childTree7.mutate().toCardinalityGroup(0, 2);
        
        IFeature childFeature8 = featureModelGroupCardinalities.mutate().addFeature("Test8");
        childTree7.mutate().addFeatureBelow(childFeature8);
        
        IFeature childFeature9 = featureModelGroupCardinalities.mutate().addFeature("Test9");
        childTree7.mutate().addFeatureBelow(childFeature9);
        
        IFeature childFeature10 = featureModelGroupCardinalities.mutate().addFeature("Test10");
        childTree7.mutate().addFeatureBelow(childFeature10);
        
        IFeature childFeature11 = featureModelGroupCardinalities.mutate().addFeature("Test11");
        IFeatureTree childTree11 = rootTree.mutate().addFeatureBelow(childFeature11);
        childTree11.mutate().makeMandatory();
        
        IFormat<IFeatureModel> format = new UVLFeatureModelFormat();
        Result<IFeatureModel> result = format.parse(new FileInputMapper(
                Path.of("src", "main", "resources", "featureModelSerializeResultWithGroupCardinalities.uvl"),
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
        Assertions.assertEquals(4, rootChildrenNames.size());
        Assertions.assertTrue(rootChildrenNames.contains("Test1"));
        Assertions.assertTrue(rootChildrenNames.contains("Test2"));
        Assertions.assertTrue(rootChildrenNames.contains("Test7"));
        Assertions.assertTrue(rootChildrenNames.contains("Test11"));

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
        Assertions.assertTrue(test7Feature.getFeatureTree().get().isOptional());
        List<String> test7ChildrenNames = test7Feature.getFeatureTree().get().getChildren().stream()
                .map((it) -> it.getFeature().getName().get())
                .collect(Collectors.toList());
        Assertions.assertEquals(3, test7ChildrenNames.size());
        Assertions.assertTrue(test7ChildrenNames.contains("Test8"));
        Assertions.assertTrue(test7ChildrenNames.contains("Test9"));
        Assertions.assertTrue(test7ChildrenNames.contains("Test10"));
        
        // testing Test8 feature
        IFeature test8Feature = parsedFeatureModel.getFeature("Test8").get();
        Group parentGroup8 = test8Feature.getFeatureTree().get().getParentGroup().get();
        Assertions.assertTrue(parentGroup8.isCardinalityGroup());
        Assertions.assertTrue(parentGroup8.getLowerBound() == 0);
        Assertions.assertTrue(parentGroup8.getUpperBound() == 2);
        Assertions.assertTrue(test8Feature.getFeatureTree().get().getChildren().isEmpty());
        
        // testing Test9 feature
        IFeature test9Feature = parsedFeatureModel.getFeature("Test9").get();
        Group parentGroup9 = test9Feature.getFeatureTree().get().getParentGroup().get();
        Assertions.assertTrue(parentGroup9.isCardinalityGroup());
        Assertions.assertTrue(parentGroup9.getLowerBound() == 0);
        Assertions.assertTrue(parentGroup9.getUpperBound() == 2);
        Assertions.assertTrue(test9Feature.getFeatureTree().get().getChildren().isEmpty());
        
        // testing Test10 feature
        IFeature test10Feature = parsedFeatureModel.getFeature("Test10").get();
        Group parentGroup10 = test10Feature.getFeatureTree().get().getParentGroup().get();
        Assertions.assertTrue(parentGroup10.isCardinalityGroup());
        Assertions.assertTrue(parentGroup10.getLowerBound() == 0);
        Assertions.assertTrue(parentGroup10.getUpperBound() == 2);
        Assertions.assertTrue(test10Feature.getFeatureTree().get().getChildren().isEmpty());
        
        // testing Test11 feature
        IFeature test11Feature = parsedFeatureModel.getFeature("Test11").get();
        Assertions.assertTrue(
                test11Feature.getFeatureTree().get().getParentGroup().get().isAnd());
        Assertions.assertTrue(test11Feature.getFeatureTree().get().isMandatory());
    }
    
    public static void testParseWithFeatureCardinality() throws IOException {
    	FeatureModel featureModel = new FeatureModel(Identifiers.newCounterIdentifier());
    	
    	// features
        IFeatureTree rootTree =
        		featureModel.mutate().addFeatureTreeRoot(
        				featureModel.mutate().addFeature("Sandwich"));
        rootTree.mutate().toAndGroup();
        
        IFeature childFeature1 = featureModel.mutate().addFeature("Bread");
        IFeatureTree childTree1 = rootTree.mutate().addFeatureBelow(childFeature1);
        childTree1.mutate().makeMandatory();
        
        IFeature childFeature2 = featureModel.mutate().addFeature("Sauce");
        IFeatureTree childTree2 = rootTree.mutate().addFeatureBelow(childFeature2);
        childTree2.mutate().toOrGroup();
        
        IFeature childFeature3 = featureModel.mutate().addFeature("Cheese");
        IFeatureTree childTree3 = rootTree.mutate().addFeatureBelow(childFeature3);
        childTree3.mutate().toOrGroup();
        
        IFeature childFeature4 = featureModel.mutate().addFeature("Pickle");
        IFeatureTree childTree4 = rootTree.mutate().addFeatureBelow(childFeature4);
        childTree4.mutate().setFeatureCardinality(Range.of(1, 3));
        
        IFeature childFeature5 = featureModel.mutate().addFeature("Ketchup");
        rootTree.mutate().addFeatureBelow(childFeature4);
        
        IFeature childFeature6 = featureModel.mutate().addFeature("Mustard");
        rootTree.mutate().addFeatureBelow(childFeature4);
        
        IFeature childFeature7 = featureModel.mutate().addFeature("Cheddar");
        rootTree.mutate().addFeatureBelow(childFeature3);
        
        IFeature childFeature8 = featureModel.mutate().addFeature("Gouda");
        rootTree.mutate().addFeatureBelow(childFeature3);
        
        IFeature childFeature9 = featureModel.mutate().addFeature("Goat");
        rootTree.mutate().addFeatureBelow(childFeature3);
        
        IFormat<IFeatureModel> format = new UVLFeatureModelFormat();
        Result<IFeatureModel> result = format.parse(new FileInputMapper(
                Path.of("src", "main", "resources", "featureModelSerializeResultWithFeatureCardinalities.uvl"),
                Charset.defaultCharset()));

        if (result.isEmpty()) {
            Assertions.fail();
        }

        IFeatureModel parsedFeatureModel = result.get();
        
        // testing Sandwich
        IFeature rootFeature = parsedFeatureModel.getFeature("Sandwich").get();
        List<String> rootChildrenNames = rootFeature.getFeatureTree().get().getChildren().stream()
                .map((it) -> it.getFeature().getName().get())
                .collect(Collectors.toList());
        Assertions.assertEquals(4, rootChildrenNames.size());
        Assertions.assertTrue(rootChildrenNames.contains("Bread"));
        Assertions.assertTrue(rootChildrenNames.contains("Sauce"));
        Assertions.assertTrue(rootChildrenNames.contains("Cheese"));
        Assertions.assertTrue(rootChildrenNames.contains("Pickle"));
        
        // testing Bread
        IFeature test1Feature = parsedFeatureModel.getFeature("Bread").get();
        Assertions.assertTrue(
                test1Feature.getFeatureTree().get().getParentGroup().get().isAnd());
        Assertions.assertTrue(test1Feature.getFeatureTree().get().isMandatory());
        
        // testing Sauce
        IFeature test2Feature = parsedFeatureModel.getFeature("Sauce").get();
        Assertions.assertTrue(
                test2Feature.getFeatureTree().get().getParentGroup().get().isAnd());
        Assertions.assertTrue(test2Feature.getFeatureTree().get().isOptional());
        List<String> test2ChildrenNames = test2Feature.getFeatureTree().get().getChildren().stream()
                .map((it) -> it.getFeature().getName().get())
                .collect(Collectors.toList());
        Assertions.assertEquals(2, test2ChildrenNames.size());
        Assertions.assertTrue(test2ChildrenNames.contains("Ketchup"));
        Assertions.assertTrue(test2ChildrenNames.contains("Mustard"));
        
        // testing Cheese
        IFeature test3Feature = parsedFeatureModel.getFeature("Cheese").get();
        Assertions.assertTrue(
                test3Feature.getFeatureTree().get().getParentGroup().get().isAnd());
        Assertions.assertTrue(test3Feature.getFeatureTree().get().isOptional());
        List<String> test3ChildrenNames = test3Feature.getFeatureTree().get().getChildren().stream()
                .map((it) -> it.getFeature().getName().get())
                .collect(Collectors.toList());
        Assertions.assertEquals(3, test3ChildrenNames.size());
        Assertions.assertTrue(test3ChildrenNames.contains("Cheddar"));
        Assertions.assertTrue(test3ChildrenNames.contains("Gouda"));
        Assertions.assertTrue(test3ChildrenNames.contains("Goat"));
        
        // testing Pickle 
        IFeature test4Feature = parsedFeatureModel.getFeature("Pickle").get();
        Assertions.assertTrue(
                test4Feature.getFeatureTree().get().getParentGroup().get().isAnd());
        Assertions.assertTrue(test4Feature.getFeatureTree().get().isOptional());
        Integer lowerBound = test4Feature.getFeatureTree().get().getFeatureCardinalityLowerBound();
        Integer upperBound = test4Feature.getFeatureTree().get().getFeatureCardinalityUpperBound();
        Assertions.assertTrue(lowerBound == 1 && upperBound == 3); 
        Assertions.assertTrue(test4Feature.getFeatureTree().get().getChildren().isEmpty());
        
        // testing Ketchup and Mustard
        IFeature test5Feature = parsedFeatureModel.getFeature("Ketchup").get();
        Assertions.assertTrue(
                test5Feature.getFeatureTree().get().getParentGroup().get().isOr());
        Assertions.assertTrue(test5Feature.getFeatureTree().get().getChildren().isEmpty());
        
        IFeature test6Feature = parsedFeatureModel.getFeature("Mustard").get();
        Assertions.assertTrue(
                test6Feature.getFeatureTree().get().getParentGroup().get().isOr());
        Assertions.assertTrue(test6Feature.getFeatureTree().get().getChildren().isEmpty());
        
        // testing Goat, Gouda and Cheddar
        IFeature test7Feature = parsedFeatureModel.getFeature("Goat").get();
        Assertions.assertTrue(
                test7Feature.getFeatureTree().get().getParentGroup().get().isOr());
        Assertions.assertTrue(test7Feature.getFeatureTree().get().getChildren().isEmpty());
        
        IFeature test8Feature = parsedFeatureModel.getFeature("Gouda").get();
        Assertions.assertTrue(
                test8Feature.getFeatureTree().get().getParentGroup().get().isOr());
        Assertions.assertTrue(test8Feature.getFeatureTree().get().getChildren().isEmpty());
        
        IFeature test9Feature = parsedFeatureModel.getFeature("Cheddar").get();
        Assertions.assertTrue(
                test9Feature.getFeatureTree().get().getParentGroup().get().isOr());
        Assertions.assertTrue(test9Feature.getFeatureTree().get().getChildren().isEmpty());
        
    }
    
    public static void testParseWithLiteralConstraint() throws IOException {
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
        IFeatureTree childFeature5Tree = childTree2.mutate().addFeatureBelow(childFeature5);
        
        Attribute<Integer> testAttribute = Attributes.get(new Name("any", "test"), Integer.class);
        childFeature5Tree.mutate().setAttributeValue(testAttribute, 5);

        IFeature childFeature6 = featureModel.mutate().addFeature("Test6");
        childTree2.mutate().addFeatureBelow(childFeature6);

        IFeature childFeature7 = featureModel.mutate().addFeature("Test7");
        IFeatureTree childTree7 = rootTree.mutate().addFeatureBelow(childFeature7);
        childTree7.mutate().makeMandatory();
        
        Object attribute = childFeature5Tree.getAttributes().get().values().stream().findFirst().get();
        
        IFormula formula1 = new Or(
                new And(new Literal("Test1"), new Literal("Test2")),
                new BiImplies(new Literal("Test3"), new Literal("Test4")),
                new Implies(new Literal("Test5"), new Literal("Test6")),
                new Not(new Literal("Test7")));

        // constraints
        featureModel.mutate().addConstraint(formula1);
    	
    	UVLFeatureModelFormat format = new UVLFeatureModelFormat();
        Result<String> featureModelString = format.serialize(featureModel);

        if (featureModelString.isEmpty()) {
            Assertions.fail();
        }

        String expected = new String(
                Files.readAllBytes(Path.of("src", "main", "resources", "featureModelSerializeResultWithLiteralConstraint.uvl")));
        Assertions.assertEquals(expected, featureModelString.get());
    }
    
    public static void testParseWithIntegerConstraint() throws IOException {
    	 IFormat<IFeatureModel> format = new UVLFeatureModelFormat();
         Result<IFeatureModel> result = format.parse(new FileInputMapper(
                 Path.of("src", "main", "resources", "featureModelSerializeResultWithIntegerConstraint.uvl"),
                 Charset.defaultCharset()));

         if (result.isEmpty()) {
             Assertions.fail();
         }

         IFeatureModel parsedFeatureModel = result.get();
    }
    
    public static void testUVLConstraintToAndFormula() throws IOException {
    	IFormat<IFeatureModel> format = new UVLFeatureModelFormat();
        Result<IFeatureModel> result = format.parse(new FileInputMapper(
                Path.of("src", "main", "resources", "UVLConstraintParser", "featureModelWithLengthAggregateExpression.uvl"),
                Charset.defaultCharset()));

        if (result.isEmpty()) {
            Assertions.fail();
        }

        IFeatureModel parsedFeatureModel = result.get();
        
        IFormula impliesFormula = new Implies(new Literal("Cheese"), new Literal("Pickle"));
        IConstraint impliesConstraint = parsedFeatureModel.getConstraints().stream().findFirst().orElse(null); 
        Assertions.assertEquals(impliesFormula, impliesConstraint.getFormula());
        
    }
    
    public static void main(String[] args) throws IOException {
    	// testParseWithGroupCardinality();
    	// testParseWithFeatureCardinality();
        // testParseWithLiteralConstraint();
    	// testParseWithIntegerConstraint();
    	testUVLConstraintToAndFormula();
    }
}


