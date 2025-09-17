/*
 * Copyright © 2025 Cyril de Catheu (cdecatheu@hey.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tech.catheu.jeamlit.doclet;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

/**
 * Custom doclet to generate JSON documentation for Jeamlit API
 * Processes only the Jt class and components package classes
 */
public class JsonDoclet implements Doclet {

    private Reporter reporter;

    @Override
    public void init(Locale locale, Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public String getName() {
        return "JsonDoclet";
    }

    @Override
    public Set<? extends Option> getSupportedOptions() {
        return Set.of();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean run(DocletEnvironment environment) {
        try {
            Map<String, Object> documentation = new HashMap<>();

            // Process all type elements (classes, interfaces)
            Set<TypeElement> allClasses = ElementFilter.typesIn(environment.getIncludedElements())
                    .stream()
                    .collect(Collectors.toSet());

            reporter.print(Diagnostic.Kind.NOTE, "Total classes found: " + allClasses.size());

            Set<TypeElement> classes = allClasses.stream()
                    .filter(this::shouldProcessClass)
                    .collect(Collectors.toSet());

            reporter.print(Diagnostic.Kind.NOTE, "Classes to process: " + classes.size());
            for (TypeElement clazz : classes) {
                reporter.print(Diagnostic.Kind.NOTE, "Processing class: " + clazz.getQualifiedName());
            }

            for (TypeElement clazz : classes) {
                processClass(clazz, documentation, environment);
            }

            // Write JSON output
            writeJsonOutput(documentation);

            reporter.print(Diagnostic.Kind.NOTE, "JsonDoclet generated documentation for " + documentation.size() + " API methods");
            return true;
        } catch (Exception e) {
            reporter.print(Diagnostic.Kind.ERROR, "Error generating JSON documentation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if a class should be processed based on our filtering criteria
     */
    private boolean shouldProcessClass(TypeElement clazz) {
        String qualifiedName = clazz.getQualifiedName().toString();

        // Process Jt class
        if ("tech.catheu.jeamlit.core.Jt".equals(qualifiedName)) {
            return true;
        }

        // Process all classes in components package
        if (qualifiedName.startsWith("tech.catheu.jeamlit.components.")) {
            return true;
        }

        return false;
    }

    /**
     * Process a single class and extract its documentation
     */
    private void processClass(TypeElement clazz, Map<String, Object> documentation, DocletEnvironment environment) {
        String className = clazz.getQualifiedName().toString();

        // Get all public methods
        List<ExecutableElement> methods = ElementFilter.methodsIn(clazz.getEnclosedElements())
                .stream()
                .filter(method -> method.getModifiers().contains(Modifier.PUBLIC))
                .collect(Collectors.toList());

        for (ExecutableElement method : methods) {
            String methodKey = generateMethodKey(className, method);
            Map<String, Object> methodDoc = processMethod(method, environment);
            if (methodDoc != null) {
                documentation.put(methodKey, methodDoc);
            }
        }
    }

    /**
     * Generate a unique key for a method (similar to streamlit.json format)
     */
    private String generateMethodKey(String className, ExecutableElement method) {
        // For Jt class, use format like "tech.catheu.jeamlit.core.Jt.text"
        // For components, use format like "ButtonComponent.onClick"
        if ("tech.catheu.jeamlit.core.Jt".equals(className)) {
            return "Jt." +  method.getSimpleName();
        } else {
            String simpleName = className.substring(className.lastIndexOf('.') + 1);
            return simpleName + "." + method.getSimpleName();
        }
    }

    /**
     * Process a single method and extract its documentation
     */
    private Map<String, Object> processMethod(ExecutableElement method, DocletEnvironment environment) {
        Map<String, Object> methodDoc = new HashMap<>();

        // Basic method information
        methodDoc.put("name", method.getSimpleName().toString());
        methodDoc.put("signature", generateSignature(method));

        // Get JavaDoc comment
        DocCommentTree docComment = environment.getDocTrees().getDocCommentTree(method);
        if (docComment != null) {
            processDocComment(docComment, methodDoc);
        }

        // Process parameters
        List<Map<String, Object>> args = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Map<String, String> paramDescriptions = (Map<String, String>) methodDoc.get("paramDescriptions");
        if (paramDescriptions == null) {
            paramDescriptions = new HashMap<>();
        }

        for (VariableElement param : method.getParameters()) {
            Map<String, Object> paramDoc = new HashMap<>();
            String paramName = param.getSimpleName().toString();
            paramDoc.put("name", paramName);
            paramDoc.put("type_name", getTypeName(param.asType()));
            paramDoc.put("is_optional", false); // Java doesn't have optional params like Python
            paramDoc.put("description", paramDescriptions.getOrDefault(paramName, ""));
            paramDoc.put("default", null);
            args.add(paramDoc);
        }

        // Remove the temporary paramDescriptions from methodDoc
        methodDoc.remove("paramDescriptions");
        methodDoc.put("args", args);

        // Process return type
        List<Map<String, Object>> returns = new ArrayList<>();
        TypeMirror returnType = method.getReturnType();
        if (!"void".equals(returnType.toString())) {
            Map<String, Object> returnDoc = new HashMap<>();
            String componentReturnType = extractComponentReturnType(method.getEnclosingElement(), returnType, environment);
            returnDoc.put("type_name", componentReturnType != null ? componentReturnType : getTypeName(returnType));
            returnDoc.put("is_generator", false);
            returnDoc.put("description", ""); // Will be filled from @return tag
            returnDoc.put("return_name", null);
            returns.add(returnDoc);
        }
        methodDoc.put("returns", returns);

        // Process builder methods for Jt class methods that return builders
        List<Map<String, Object>> builderMethods = extractBuilderMethods(method.getEnclosingElement(), returnType, environment);
        if (builderMethods != null && !builderMethods.isEmpty()) {
            methodDoc.put("builderMethods", builderMethods);
        }

        // TODO: Add source GitHub link
        methodDoc.put("source", "");

        return methodDoc;
    }

    /**
     * Process JavaDoc comment and extract description, examples, etc.
     */
    private void processDocComment(DocCommentTree docComment, Map<String, Object> methodDoc) {
        // Get full body description
        List<? extends DocTree> fullBody = docComment.getFullBody();
        String description = fullBody.stream()
                .map(DocTree::toString)
                .collect(Collectors.joining(" "))
                .trim();

        methodDoc.put("description", description);

        // Extract parameter descriptions from @param tags
        final Map<String, String> paramDescriptions = new HashMap<>();

        // Process block tags (@param, @return, etc.)
        for (DocTree blockTag : docComment.getBlockTags()) {
            if (blockTag instanceof ParamTree paramTree) {
                String paramName = paramTree.getName().toString();
                String paramDescription = paramTree.getDescription().stream()
                        .map(DocTree::toString)
                        .collect(Collectors.joining(" "))
                        .trim();
                paramDescriptions.put(paramName, paramDescription);
            } else if (blockTag instanceof ReturnTree returnTree) {
                // TODO: Update return description
            }
        }

        // Store parameter descriptions for later use
        methodDoc.put("paramDescriptions", paramDescriptions);
    }

    /**
     * Generate method signature string
     */
    private String generateSignature(ExecutableElement method) {
        StringBuilder sig = new StringBuilder();
        sig.append(method.getSimpleName()).append("(");

        List<String> paramStrings = new ArrayList<>();
        for (VariableElement param : method.getParameters()) {
            String paramString = param.getSimpleName().toString();
            // Add type information for clarity
            paramString = getSimpleTypeName(param.asType()) + " " + paramString;
            paramStrings.add(paramString);
        }

        sig.append(String.join(", ", paramStrings));
        sig.append(")");

        return sig.toString();
    }

    /**
     * Get type name for documentation
     */
    private String getTypeName(TypeMirror type) {
        return type.toString();
    }

    /**
     * Get simple type name without package
     */
    private String getSimpleTypeName(TypeMirror type) {
        String fullName = type.toString();
        // Remove generics for simplicity
        int genericStart = fullName.indexOf('<');
        if (genericStart > 0) {
            fullName = fullName.substring(0, genericStart);
        }
        // Get simple name
        int lastDot = fullName.lastIndexOf('.');
        return lastDot > 0 ? fullName.substring(lastDot + 1) : fullName;
    }

    /**
     * Extract the component return type (the T in JtComponent<T>) for methods from the Jt class
     */
    private String extractComponentReturnType(Element classElement, TypeMirror returnType, DocletEnvironment environment) {
        if (!(classElement instanceof TypeElement)) {
            return null;
        }

        TypeElement typeElement = (TypeElement) classElement;
        String className = typeElement.getQualifiedName().toString();

        // Only process methods from the Jt class
        if (!"tech.catheu.jeamlit.core.Jt".equals(className)) {
            return null;
        }

        // Check if the return type is a component builder
        if (!(returnType instanceof DeclaredType)) {
            return null;
        }

        DeclaredType declaredReturnType = (DeclaredType) returnType;
        TypeElement returnTypeElement = (TypeElement) declaredReturnType.asElement();
        String returnClassName = returnTypeElement.getQualifiedName().toString();

        // Check if this is a Builder class in the components package
        if (!returnClassName.contains(".components.") || !returnClassName.endsWith(".Builder")) {
            return null;
        }

        // Get the component class (remove .Builder from the end)
        String componentClassName = returnClassName.substring(0, returnClassName.length() - ".Builder".length());

        // Find the component class
        Set<TypeElement> allClasses = ElementFilter.typesIn(environment.getIncludedElements())
                .stream()
                .collect(Collectors.toSet());

        TypeElement componentClass = allClasses.stream()
                .filter(clazz -> clazz.getQualifiedName().toString().equals(componentClassName))
                .findFirst()
                .orElse(null);

        if (componentClass == null) {
            return null;
        }

        // Look for JtComponent<T> in the component's superclass hierarchy
        TypeMirror superclass = componentClass.getSuperclass();
        while (superclass != null && !"java.lang.Object".equals(superclass.toString())) {
            if (superclass instanceof DeclaredType) {
                DeclaredType declaredType = (DeclaredType) superclass;
                TypeElement superElement = (TypeElement) declaredType.asElement();

                // Check if this is JtComponent
                if ("tech.catheu.jeamlit.core.JtComponent".equals(superElement.getQualifiedName().toString())) {
                    // Extract the generic type parameter T
                    List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                    if (!typeArguments.isEmpty()) {
                        TypeMirror componentType = typeArguments.get(0);
                        return convertToSimpleType(componentType.toString());
                    }
                }

                // Continue up the hierarchy
                superclass = superElement.getSuperclass();
            } else {
                break;
            }
        }

        return null;
    }

    /**
     * Convert complex types to simple types for documentation
     */
    private String convertToSimpleType(String typeName) {
        // Handle JtComponent.NONE
        if (typeName.contains("JtComponent.NONE")) {
            return "void";
        }

        // Handle primitives and wrapper types
        switch (typeName) {
            case "java.lang.Boolean":
            case "Boolean":
                return "boolean";
            case "java.lang.String":
            case "String":
                return "string";
            case "java.lang.Integer":
            case "Integer":
                return "int";
            case "java.lang.Double":
            case "Double":
                return "double";
            case "java.lang.Float":
            case "Float":
                return "float";
            case "java.lang.Long":
            case "Long":
                return "long";
            default:
                // For other types, return the simple name without package
                int lastDot = typeName.lastIndexOf('.');
                return lastDot > 0 ? typeName.substring(lastDot + 1) : typeName;
        }
    }

    /**
     * Extract builder methods for Jt class methods that return component builders
     */
    private List<Map<String, Object>> extractBuilderMethods(Element classElement, TypeMirror returnType, DocletEnvironment environment) {
        if (!(classElement instanceof TypeElement)) {
            return null;
        }

        TypeElement typeElement = (TypeElement) classElement;
        String className = typeElement.getQualifiedName().toString();

        // Only process methods from the Jt class
        if (!"tech.catheu.jeamlit.core.Jt".equals(className)) {
            return null;
        }

        // Check if the return type is a component builder
        if (!(returnType instanceof DeclaredType)) {
            return null;
        }

        DeclaredType declaredReturnType = (DeclaredType) returnType;
        TypeElement returnTypeElement = (TypeElement) declaredReturnType.asElement();
        String returnClassName = returnTypeElement.getQualifiedName().toString();

        // Check if this is a Builder class in the components package
        if (!returnClassName.contains(".components.") || !returnClassName.endsWith(".Builder")) {
            return null;
        }

        // Extract builder methods
        List<Map<String, Object>> builderMethods = new ArrayList<>();
        List<ExecutableElement> methods = ElementFilter.methodsIn(returnTypeElement.getEnclosedElements())
                .stream()
                .filter(method -> method.getModifiers().contains(Modifier.PUBLIC))
                .filter(method -> !"<init>".equals(method.getSimpleName().toString()))// Exclude constructors
                .collect(Collectors.toList());

        for (ExecutableElement builderMethod : methods) {
            Map<String, Object> builderMethodDoc = new HashMap<>();

            // Basic method information
            builderMethodDoc.put("name", builderMethod.getSimpleName().toString());
            builderMethodDoc.put("signature", generateSignature(builderMethod));

            // Get JavaDoc comment
            DocCommentTree docComment = environment.getDocTrees().getDocCommentTree(builderMethod);
            if (docComment != null) {
                processDocComment(docComment, builderMethodDoc);
            } else {
                builderMethodDoc.put("description", "");
            }

            // Process parameters
            List<Map<String, Object>> args = new ArrayList<>();
            for (VariableElement param : builderMethod.getParameters()) {
                Map<String, Object> paramDoc = new HashMap<>();
                paramDoc.put("name", param.getSimpleName().toString());
                paramDoc.put("type_name", getTypeName(param.asType()));
                paramDoc.put("is_optional", false);
                paramDoc.put("description", "");
                paramDoc.put("default", null);
                args.add(paramDoc);
            }
            builderMethodDoc.put("args", args);

            // Process return type
            List<Map<String, Object>> returns = new ArrayList<>();
            TypeMirror builderReturnType = builderMethod.getReturnType();
            if (!"void".equals(builderReturnType.toString())) {
                Map<String, Object> returnDoc = new HashMap<>();
                returnDoc.put("type_name", getTypeName(builderReturnType));
                returnDoc.put("is_generator", false);
                returnDoc.put("description", "");
                returnDoc.put("return_name", null);
                returns.add(returnDoc);
            }
            builderMethodDoc.put("returns", returns);

            builderMethodDoc.put("source", "");

            builderMethods.add(builderMethodDoc);
        }

        return builderMethods.isEmpty() ? null : builderMethods;
    }

    /**
     * Write the documentation to JSON file
     */
    private void writeJsonOutput(Map<String, Object> documentation) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        try (FileWriter writer = new FileWriter("jeamlit.json")) {
            objectMapper.writeValue(writer, documentation);
        }
    }
}
