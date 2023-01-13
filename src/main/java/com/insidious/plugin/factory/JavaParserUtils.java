package com.insidious.plugin.factory;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JavaParserUtils {
    private static final Logger logger = LoggerUtil.getInstance(JavaParserUtils.class);

    public static void mergeCompilationUnits(CompilationUnit destinationUnit, CompilationUnit fromUnit) {

        for (TypeDeclaration<?> type : fromUnit.getTypes()) {

            Optional<ClassOrInterfaceDeclaration> existingTypeDeclaration = destinationUnit.getClassByName(
                    type.getName()
                            .asString());
            if (existingTypeDeclaration.isEmpty()) {
                destinationUnit.addType(type);
            } else {
                ClassOrInterfaceDeclaration existingType = existingTypeDeclaration.get();
                mergeTypeDeclaration(existingType, type);
            }


        }

        for (ImportDeclaration anImport : fromUnit.getImports()) {
            destinationUnit.addImport(anImport);
        }


    }

    private static void mergeTypeDeclaration(ClassOrInterfaceDeclaration existingType, TypeDeclaration<?> type) {

        // merge fields

        for (FieldDeclaration field : type.getFields()) {

            Optional<FieldDeclaration> existingField = existingType.getFieldByName(field.getVariables()
                    .get(0)
                    .getName()
                    .asString());
            if (existingField.isEmpty()) {
                existingType.addMember(field);
            } else {
                // field already exists
            }

        }


        // merge methods

        for (MethodDeclaration newMethod : type.getMethods()) {

            String methodName = newMethod.getNameAsString();
            List<MethodDeclaration> existingMethod = existingType.getMethodsByName(methodName);

            if (existingMethod.size() == 0) {
                // method does not exist, just add
                existingType.addMember(newMethod);
            } else {
                MethodDeclaration existingMethodDeclaration = existingMethod.get(0);
                if (methodName.equals("setup") || methodName.equals("finished")) {
                    // merge methods
                    mergeMethods(existingMethodDeclaration, newMethod);
                } else {

                    // a test method with same name exists, so we create a new name for new test method
                    int i = 0;
                    String newMethodName = "";
                    while (existingMethod.size() > 0) {
                        i++;
                        newMethodName = methodName + i;
                        existingMethod = existingType.getMethodsByName(newMethodName);
                    }
                    newMethod.setName(newMethodName);
                    existingType.addMember(newMethod);
                }
            }

        }
    }

    private static void mergeMethods(MethodDeclaration existingMethodDeclaration, MethodDeclaration newMethod) {
        Map<String, Boolean> exitingStatementMap = new HashMap<>();
        BlockStmt methodBodyBlock = existingMethodDeclaration.getBody()
                .get();
        NodeList<Statement> existingStatements = methodBodyBlock
                .getStatements();
        for (Statement existingStatement : existingStatements) {
            if (existingStatement.isExpressionStmt()) {
                String key = existingStatement.asExpressionStmt()
                        .getExpression()
                        .toString();
                if (key.startsWith("//") && key.contains("\n")) {
                    key = existingStatement.toString()
                            .split("\n")[1];
                }
                exitingStatementMap.put(key, true);
            } else {
                logger.warn("This is a try statement: " + existingStatement);
            }
        }


        NodeList<Statement> newStatements = newMethod.getBody()
                .get()
                .getStatements();

        NodeList<Statement> statementsToAdd = new NodeList<Statement>();
        for (Statement newStatement : newStatements) {
            if (newStatement.isExpressionStmt()) {
                if (exitingStatementMap.containsKey(newStatement.asExpressionStmt()
                        .getExpression()
                        .toString())) {
                    continue;
                }
                statementsToAdd.add(newStatement);
            } else {
                logger.warn("This is not an expression statement: " + newStatement);
            }

        }

        for (Statement statement : statementsToAdd) {
            methodBodyBlock.addStatement(statement);
        }


    }
}
