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
package io.jeamlit.doclet;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.source.doctree.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

/**
 * Visitor to convert DocTree elements to HTML format
 */
class HtmlDocTreeVisitor implements DocTreeVisitor<String, Void> {

    @Override
    public String visitAttribute(AttributeTree node, Void p) {
        return node.toString(); // Keep attributes as-is
    }

    @Override
    public String visitAuthor(AuthorTree node, Void p) {
        return visitOther(node, p);
    }

    @Override
    public String visitComment(CommentTree node, Void p) {
        return "<!-- " + node.getBody() + " -->";
    }

    @Override
    public String visitDeprecated(DeprecatedTree node, Void p) {
        return visitOther(node, p);
    }

    @Override
    public String visitDocComment(DocCommentTree node, Void p) {
        StringBuilder result = new StringBuilder();
        for (DocTree tree : node.getFullBody()) {
            result.append(tree.accept(this, p));
        }
        return result.toString();
    }

    @Override
    public String visitDocRoot(DocRootTree node, Void p) {
        return ""; // Handle doc root if needed
    }

    @Override
    public String visitEndElement(EndElementTree node, Void p) {
        return "</" + node.getName() + ">";
    }

    @Override
    public String visitEntity(EntityTree node, Void p) {
        return "&" + node.getName() + ";";
    }

    @Override
    public String visitErroneous(ErroneousTree node, Void p) {
        return node.getBody();
    }

    @Override
    public String visitIdentifier(IdentifierTree node, Void p) {
        return node.getName().toString();
    }

    @Override
    public String visitInheritDoc(InheritDocTree node, Void p) {
        return ""; // Handle inherit doc if needed
    }

    @Override
    public String visitLink(LinkTree node, Void p) {
        // Convert {@link Class} to HTML link - simplified for now
        StringBuilder result = new StringBuilder("<code>");
        result.append(node.getReference().accept(this, p));
        if (!node.getLabel().isEmpty()) {
            result.append(" ");
            for (DocTree tree : node.getLabel()) {
                result.append(tree.accept(this, p));
            }
        }
        result.append("</code>");
        return result.toString();
    }

    @Override
    public String visitLiteral(LiteralTree node, Void p) {
        // Convert {@code text} to <code>text</code>
        return "<code>" + escapeHtml(node.getBody().getBody()) + "</code>";
    }

    @Override
    public String visitParam(ParamTree node, Void p) {
        return visitOther(node, p);
    }

    @Override
    public String visitReference(ReferenceTree node, Void p) {
        return node.getSignature();
    }

    @Override
    public String visitReturn(ReturnTree node, Void p) {
        return visitOther(node, p);
    }

    @Override
    public String visitSee(SeeTree node, Void p) {
        return visitOther(node, p);
    }

    @Override
    public String visitSerial(SerialTree node, Void p) {
        return visitOther(node, p);
    }

    @Override
    public String visitSerialData(SerialDataTree node, Void p) {
        return visitOther(node, p);
    }

    @Override
    public String visitSerialField(SerialFieldTree node, Void p) {
        return visitOther(node, p);
    }

    @Override
    public String visitSince(SinceTree node, Void p) {
        return visitOther(node, p);
    }

    @Override
    public String visitStartElement(StartElementTree node, Void p) {
        StringBuilder result = new StringBuilder("<" + node.getName());
        for (DocTree attr : node.getAttributes()) {
            result.append(" ").append(attr.accept(this, p));
        }
        if (node.isSelfClosing()) {
            result.append(" />");
        } else {
            result.append(">");
        }
        return result.toString();
    }

    @Override
    public String visitText(TextTree node, Void p) {
        return escapeHtml(node.getBody());
    }

    @Override
    public String visitThrows(ThrowsTree node, Void p) {
        return visitOther(node, p);
    }

    @Override
    public String visitUnknownBlockTag(UnknownBlockTagTree node, Void p) {
        return visitOther(node, p);
    }

    @Override
    public String visitUnknownInlineTag(UnknownInlineTagTree node, Void p) {
        return visitOther(node, p);
    }

    @Override
    public String visitValue(ValueTree node, Void p) {
        return visitOther(node, p);
    }

    @Override
    public String visitVersion(VersionTree node, Void p) {
        return visitOther(node, p);
    }

    @Override
    public String visitSnippet(SnippetTree node, Void p) {
        // Convert @snippet tags to <pre><code> HTML blocks
        StringBuilder result = new StringBuilder();
        result.append("<pre><code class=\"language-java\">");

        // Get the snippet body - it's a TextTree, not a List
        TextTree body = node.getBody();
        if (body != null) {
            String content = body.getBody();
            // Remove leading asterisks from JavaDoc formatting and trim
            content = content.replaceAll("(?m)^\\s*\\*\\s?", "");
            // HTML escape the content
            content = content.replace("&", "&amp;")
                             .replace("<", "&lt;")
                             .replace(">", "&gt;");
            result.append(content);
        }

        result.append("</code></pre>");
        return result.toString();
    }

    @Override
    public String visitOther(DocTree node, Void p) {
        // Most block tags are handled by their specific visitor methods
        // Fallback to string representation for unhandled cases
        return node.toString();
    }

    /**
     * Escape HTML special characters
     */
    String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

/**
 * Custom doclet to generate JSON documentation for Jeamlit API
 * Processes only the Jt class and components package classes
 */
public class JsonDoclet implements Doclet {

    private Reporter reporter;
    private final HtmlDocTreeVisitor htmlVisitor = new HtmlDocTreeVisitor();

    //method names that should point to another method's documentation
    private static final Map<String, String> METHOD_ALIASES = Map.of(
            "Jt.tableFromListColumns", "Jt.table",
            "Jt.tableFromArrayColumns", "Jt.table"
    );

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
            Set<TypeElement> allClasses = ElementFilter
                    .typesIn(environment.getIncludedElements())
                    .stream()
                    .collect(Collectors.toSet());

            reporter.print(Diagnostic.Kind.NOTE, "Total classes found: " + allClasses.size());

            Set<TypeElement> classes = allClasses.stream().filter(this::shouldProcessClass).collect(Collectors.toSet());

            reporter.print(Diagnostic.Kind.NOTE, "Classes to process: " + classes.size());
            for (TypeElement clazz : classes) {
                reporter.print(Diagnostic.Kind.NOTE, "Processing class: " + clazz.getQualifiedName());
            }

            for (TypeElement clazz : classes) {
                processClass(clazz, documentation, environment);
            }

            // Write JSON output
            writeJsonOutput(documentation);

            reporter.print(Diagnostic.Kind.NOTE,
                           "JsonDoclet generated documentation for " + documentation.size() + " API methods");
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
        if ("io.jeamlit.core.Jt".equals(qualifiedName)) {
            return true;
        }

        // Process all classes in components package
        if (qualifiedName.startsWith("io.jeamlit.components.")) {
            return true;
        }

        return false;
    }

    /**
     * Process a single class and extract its documentation
     */
    private void processClass(TypeElement clazz, Map<String, Object> documentation, DocletEnvironment environment) {
        final String className = clazz.getQualifiedName().toString();

        // Get all public methods
        final List<ExecutableElement> methods = ElementFilter
                .methodsIn(clazz.getEnclosedElements())
                .stream()
                .filter(method -> method.getModifiers().contains(Modifier.PUBLIC))
                .toList();

        for (final ExecutableElement method : methods) {
            final String methodKey = generateMethodKey(className, method);
            if (methodKey != null) {
                // Check if this method is an alias
                final String targetMethodKey = METHOD_ALIASES.getOrDefault(methodKey, methodKey);

                final Map<String, Object> methodDoc = processMethod(method, environment);

                // Check if this method name already exists (overload)
                if (documentation.containsKey(targetMethodKey)) {
                    @SuppressWarnings("unchecked") Map<String, Object> existingMethod = (Map<String, Object>) documentation.get(
                            targetMethodKey);

                    // Combine examples from the new overload if present
                    String newExamples = (String) methodDoc.get("examples");
                    if (newExamples != null && !newExamples.isEmpty()) {
                        String existingExamples = (String) existingMethod.get("examples");
                        if (existingExamples != null && !existingExamples.isEmpty()) {
                            // Combine with existing examples, separated by double newline
                            existingMethod.put("examples", existingExamples + "\n\n" + newExamples);
                        } else {
                            // No existing examples, use new ones
                            existingMethod.put("examples", newExamples);
                        }
                    }

                    // Get overloads array (should always exist since we create it)
                    @SuppressWarnings("unchecked") List<Map<String, Object>> overloads = (List<Map<String, Object>>) existingMethod.get(
                            "overloads");
                    if (overloads == null) {
                        throw new IllegalStateException("Overloads array should always exist for method: " + targetMethodKey);
                    }

                    // Add new overload
                    Map<String, Object> newOverload = new HashMap<>();
                    newOverload.put("args", methodDoc.get("args"));
                    newOverload.put("signature", methodDoc.get("signature"));
                    overloads.add(newOverload);

                } else {
                    // First occurrence of this method name
                    // Store in new format with overloads array
                    List<Map<String, Object>> overloads = new ArrayList<>();
                    Map<String, Object> firstOverload = new HashMap<>();
                    firstOverload.put("args", methodDoc.get("args"));
                    firstOverload.put("signature", methodDoc.get("signature"));
                    overloads.add(firstOverload);

                    methodDoc.put("overloads", overloads);
                    methodDoc.remove("args");
                    methodDoc.remove("signature");

                    documentation.put(targetMethodKey, methodDoc);
                }
            }
        }
    }

    /**
     * Generate a unique key for a method (similar to streamlit.json format)
     */
    private @Nullable String generateMethodKey(String className, ExecutableElement method) {
        // For Jt class, use format like "io.jeamlit.core.Jt.text"
        // For components, use format like "ButtonComponent.onClick"
        if ("io.jeamlit.core.Jt".equals(className)) {
            return "Jt." + method.getSimpleName();
        }
        return null;
    }

    /**
     * Process a single method and extract its documentation
     */
    private @Nonnull Map<String, Object> processMethod(ExecutableElement method, DocletEnvironment environment) {
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
        @SuppressWarnings("unchecked") Map<String, String> paramDescriptions = (Map<String, String>) methodDoc.get(
                "paramDescriptions");
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
            String componentReturnType = extractComponentReturnType(method.getEnclosingElement(),
                                                                    returnType,
                                                                    environment);
            final String typeName = componentReturnType != null ? componentReturnType : getTypeName(returnType);
            returnDoc.put("type_name", typeName);
            returnDoc.put("is_generator", false);
            // not filled from the @return tag - it would break the semantic of return tag in the html javadoc
            // for the moment hardcoded to a good enough value
            returnDoc.put("description", "The current %s value of the component.".formatted(typeName));
            returnDoc.put("return_name", null);
            returns.add(returnDoc);
        }
        methodDoc.put("returns", returns);

        // Process builder methods for Jt class methods that return builders
        List<Map<String, Object>> builderMethods = extractBuilderMethods(method.getEnclosingElement(),
                                                                         returnType,
                                                                         environment);
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
        // Get full body description using HTML visitor
        List<? extends DocTree> fullBody = docComment.getFullBody();
        String fullDescription = fullBody
                .stream()
                .map(tree -> tree.accept(htmlVisitor, null))
                .collect(Collectors.joining(""))
                .trim();

        // Split on "Examples:" marker at beginning of line
        String[] parts = fullDescription.split("\n Examples:");

        // Part before marker goes to "description"
        String description = parts[0].trim();
        if (!description.isEmpty()) {
            description = "<p>" + description + "</p>";
            methodDoc.put("description", description);
        }

        // Part after marker goes to "examples"
        if (parts.length > 1) {
            String examples = parts[1].trim();
            if (!examples.isEmpty()) {
                examples = "<p>" + examples + "</p>";
                methodDoc.put("examples", examples);
            }
        }

        // Extract parameter descriptions from @param tags
        final Map<String, String> paramDescriptions = new HashMap<>();

        // Process block tags (@param, @return, etc.)
        for (DocTree blockTag : docComment.getBlockTags()) {
            if (blockTag instanceof ParamTree paramTree) {
                String paramName = paramTree.getName().toString();
                String paramDescription = paramTree
                        .getDescription()
                        .stream()
                        .map(tree -> tree.accept(htmlVisitor, null))
                        .collect(Collectors.joining(""))
                        .trim();
                // Wrap parameter description in <p> tag to match Streamlit format
                if (!paramDescription.isEmpty()) {
                    paramDescription = "<p>" + paramDescription + "</p>";
                }
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
        if ("Jt".equals(method.getEnclosingElement().getSimpleName().toString())) {
            sig.append("Jt.");
        }
        sig.append(method.getSimpleName()).append("(");

        List<String> paramStrings = new ArrayList<>();
        for (VariableElement param : method.getParameters()) {
            String paramString = param.getSimpleName().toString();
            // Add annotations and type information for clarity
            // annotations not written for the moment - it was making the doc less readable
            String annotations = ""; // formatAnnotations(param);
            paramString = annotations + getTypeNameWithGenerics(param.asType()) + " " + paramString;
            paramStrings.add(paramString);
        }

        sig.append(String.join(", ", paramStrings));
        sig.append(")");

        return sig.toString();
    }

    /**
     * Format annotations for a parameter into a readable string
     */
    @SuppressWarnings("unused") // currently tested
    private String formatAnnotations(VariableElement param) {
        StringBuilder annotations = new StringBuilder();

        for (AnnotationMirror annotation : param.getAnnotationMirrors()) {
            String annotationName = annotation.getAnnotationType().asElement().getSimpleName().toString();

            // Include relevant annotations
            if (isRelevantAnnotation(annotationName)) {
                annotations.append("@").append(annotationName);

                // Handle annotation values (e.g., @Language("Markdown"))
                if (!annotation.getElementValues().isEmpty()) {
                    annotations.append("(");
                    List<String> values = new ArrayList<>();
                    annotation.getElementValues().forEach((key, value) -> {
                        String valueStr = value.toString();
                        // Keep the value as-is since it already contains proper formatting
                        values.add(valueStr);
                    });
                    annotations.append(String.join(", ", values));
                    annotations.append(")");
                }

                annotations.append(" ");
            }
        }

        return annotations.toString();
    }

    /**
     * Check if an annotation should be included in the signature
     */
    private boolean isRelevantAnnotation(String annotationName) {
        return "Nonnull".equals(annotationName) || "Nullable".equals(annotationName) || "Language".equals(annotationName) || "NotNull".equals(
                annotationName);
    }

    /**
     * Clean annotations from type names
     */
    private String cleanAnnotations(String typeName) {
        // Remove annotations with full package names (e.g., @org.jetbrains.annotations.NotNull)
        typeName = typeName.replaceAll("@[a-zA-Z0-9_.]+\\.[A-Z][a-zA-Z0-9_]*\\s*", "");
        // Remove simple annotations (e.g., @NotNull, @Nullable)
        typeName = typeName.replaceAll("@[A-Z][a-zA-Z0-9_]*\\s*", "");
        // Clean up extra spaces and commas
        typeName = typeName.replaceAll("\\s*,\\s*", ", ");
        typeName = typeName.replaceAll("\\s+", " ");
        return typeName.trim();
    }

    /**
     * Get type name for documentation with HTML escaping
     */
    private String getTypeName(TypeMirror type) {
        String typeName = type
                .toString()
                .replace("java.util.function.", "")
                .replace("java.lang.", "")
                .replace("java.util.", "");
        typeName = cleanAnnotations(typeName);
        return htmlVisitor.escapeHtml(typeName);
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
     * Get type name with generics, simplifying package names and HTML escaping
     */
    private String getTypeNameWithGenerics(TypeMirror type) {
        String typeName = type
                .toString()
                .replace("java.lang.", "")
                .replace("java.util.", "")
                .replace("io.jeamlit.core.", "")
                .replace("io.jeamlit.components.multipage.", "");
        typeName = cleanAnnotations(typeName);
        return htmlVisitor.escapeHtml(typeName);
    }

    /**
     * Extract the component return type (the T in JtComponent<T>) for methods from the Jt class
     */
    private String extractComponentReturnType(Element classElement,
                                              TypeMirror returnType,
                                              DocletEnvironment environment) {
        if (!(classElement instanceof TypeElement typeElement)) {
            return null;
        }

        String className = typeElement.getQualifiedName().toString();

        // Only process methods from the Jt class
        if (!"io.jeamlit.core.Jt".equals(className)) {
            return null;
        }

        // Check if the return type is a component builder
        if (!(returnType instanceof DeclaredType declaredReturnType)) {
            return null;
        }

        TypeElement returnTypeElement = (TypeElement) declaredReturnType.asElement();
        String returnClassName = returnTypeElement.getQualifiedName().toString();

        // Check if this is a Builder class in the components package
        if (!returnClassName.contains(".components.") || !returnClassName.endsWith(".Builder")) {
            return null;
        }

        // Get the component class (remove .Builder from the end)
        String componentClassName = returnClassName.substring(0, returnClassName.length() - ".Builder".length());

        // Find the component class
        Set<TypeElement> allClasses = ElementFilter
                .typesIn(environment.getIncludedElements())
                .stream()
                .collect(Collectors.toSet());

        TypeElement componentClass = allClasses
                .stream()
                .filter(clazz -> clazz.getQualifiedName().toString().equals(componentClassName))
                .findFirst()
                .orElse(null);

        if (componentClass == null) {
            return null;
        }

        // Look for JtComponent<T> in the component's superclass hierarchy
        TypeMirror superclass = componentClass.getSuperclass();
        while (superclass != null && !"java.lang.Object".equals(superclass.toString())) {
            if (superclass instanceof DeclaredType declaredType) {
                TypeElement superElement = (TypeElement) declaredType.asElement();

                // Check if this is JtComponent
                if ("io.jeamlit.core.JtComponent".equals(superElement.getQualifiedName().toString())) {
                    // Extract the generic type parameter T
                    List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                    if (!typeArguments.isEmpty()) {
                        TypeMirror componentType = typeArguments.getFirst();
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
     * Get all builder methods including inherited ones from JtComponentBuilder
     */
    private List<ExecutableElement> getAllBuilderMethods(TypeElement builderClass, DocletEnvironment environment) {
        final List<ExecutableElement> allMethods = new ArrayList<>();
        final Set<String> methodsToIgnore = Set.of("generateKeyForInteractive", "ensureIsValidIcon", "build");

        // Start with the concrete builder class
        TypeElement currentClass = builderClass;

        while (currentClass != null) {
            // Get methods declared in this class
            List<ExecutableElement> classMethods = ElementFilter.methodsIn(currentClass.getEnclosedElements());
            allMethods.addAll(classMethods);

            // Move to superclass
            TypeMirror superclass = currentClass.getSuperclass();
            if (superclass instanceof DeclaredType declaredSuperclass) {
                Element superElement = declaredSuperclass.asElement();
                if (superElement instanceof TypeElement) {
                    currentClass = (TypeElement) superElement;

                    // Stop if we've reached JtComponentBuilder or Object
                    String superClassName = currentClass.getQualifiedName().toString();
                    if ("io.jeamlit.core.JtComponentBuilder".equals(superClassName)) {
                        // Include JtComponentBuilder methods but filter out internal ones
                        List<ExecutableElement> parentMethods = ElementFilter
                                .methodsIn(currentClass.getEnclosedElements())
                                .stream()
                                .filter(method -> !methodsToIgnore.contains(method.getSimpleName().toString()))
                                .toList();
                        allMethods.addAll(parentMethods);
                        break;
                    } else if ("java.lang.Object".equals(superClassName)) {
                        break;
                    }
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        return allMethods;
    }

    /**
     * Extract builder methods for Jt class methods that return component builders
     */
    private List<Map<String, Object>> extractBuilderMethods(Element classElement,
                                                            TypeMirror returnType,
                                                            DocletEnvironment environment) {
        if (!(classElement instanceof TypeElement typeElement)) {
            return null;
        }

        String className = typeElement.getQualifiedName().toString();

        // Only process methods from the Jt class
        if (!"io.jeamlit.core.Jt".equals(className)) {
            return null;
        }

        // Check if the return type is a component builder
        if (!(returnType instanceof DeclaredType declaredReturnType)) {
            return null;
        }

        TypeElement returnTypeElement = (TypeElement) declaredReturnType.asElement();
        String returnClassName = returnTypeElement.getQualifiedName().toString();

        // Check if this is a Builder class in the components package
        if (!returnClassName.contains(".components.") || !returnClassName.endsWith(".Builder")) {
            return null;
        }

        // Extract builder methods
        List<Map<String, Object>> builderMethods = new ArrayList<>();
        List<ExecutableElement> methods = getAllBuilderMethods(returnTypeElement, environment)
                .stream()
                .filter(method -> method.getModifiers().contains(Modifier.PUBLIC))
                .filter(method -> !method.getModifiers().contains(Modifier.STATIC))// Exclude static methods
                .filter(method -> !"<init>".equals(method.getSimpleName().toString()))// Exclude constructors
                .filter(method -> !"build".equals(method.getSimpleName().toString()))// Exclude build method
                .toList();

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
                final String typeName = getTypeName(builderReturnType);
                returnDoc.put("type_name", typeName);
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

        try (FileWriter writer = new FileWriter("jeamlit.json", StandardCharsets.UTF_8)) {
            objectMapper.writeValue(writer, documentation);
        }
    }
}
