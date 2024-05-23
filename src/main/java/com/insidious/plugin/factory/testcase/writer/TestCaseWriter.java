package com.insidious.plugin.factory.testcase.writer;

import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.Descriptor;
import com.insidious.common.weaver.EventType;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.FieldAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.insidious.plugin.adapter.java.JavaClassAdapter;
import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.TestAssertion;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.util.ClassTypeUtils;
import com.insidious.plugin.util.ClassUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.StringUtils;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.tree.java.PsiExpressionListImpl;
import com.intellij.psi.impl.source.tree.java.PsiJavaTokenImpl;
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl;
import com.intellij.psi.impl.source.tree.java.PsiThisExpressionImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import org.objectweb.asm.Opcodes;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class TestCaseWriter {

    private static final Logger logger = LoggerUtil.getInstance(TestCaseWriter.class);
    private static final Random random = new Random(new Date().getTime());

    private static String psiTypeToJvmType(String canonicalText, boolean isReturnParameter) {
        if (canonicalText.endsWith("[]")) {
            canonicalText = psiTypeToJvmType(canonicalText.substring(0, canonicalText.length() - 2), isReturnParameter);
            return "[" + canonicalText;
        }
        switch (canonicalText) {
            case "void":
                canonicalText = "V";
                break;
            case "boolean":
                canonicalText = "Z";
                break;
            case "byte":
                canonicalText = "B";
                break;
            case "char":
                canonicalText = "C";
                break;
            case "short":
                canonicalText = "S";
                break;
            case "int":
                canonicalText = "I";
                break;
            case "long":
                canonicalText = "J";
                break;
            case "float":
                canonicalText = "F";
                break;
            case "double":
                canonicalText = "D";
                break;
            case "java.util.Map":
                if (!isReturnParameter) {
                    canonicalText = "java.util.HashMap";
                }
                break;
            case "java.util.List":
                if (!isReturnParameter) {
                    canonicalText = "java.util.ArrayList";
                }
                break;
            case "java.util.Set":
                if (!isReturnParameter) {
                    canonicalText = "java.util.HashSet";
                }
                break;
            case "java.util.Collection":
                if (!isReturnParameter) {
                    canonicalText = "java.util.ArrayList";
                }
                break;
            default:
        }
        return canonicalText;
    }

    static public void setParameterTypeFromPsiType(Parameter parameter, PsiType psiType, boolean isReturnParameter) {
        if (psiType instanceof PsiClassType) {
            PsiClassType returnClassType = (PsiClassType) psiType;
            if (returnClassType.getCanonicalText().equals(returnClassType.getName())) {
                logger.warn("return class type canonical text[" + returnClassType.getCanonicalText()
                        + "] is same as its name [" + returnClassType.getName() + "]");
                // this is a generic template type <T>, and not a real class
                parameter.setTypeForced("java.lang.Object");
                return;
            }
            parameter.setTypeForced(psiTypeToJvmType(returnClassType.rawType().getCanonicalText(), isReturnParameter));
            if (returnClassType.hasParameters()) {
                ClassUtils.extractTemplateMap(returnClassType, parameter.getTemplateMap());
                parameter.setContainer(true);
            }
        } else {
            parameter.setTypeForced(psiTypeToJvmType(psiType.getCanonicalText(), isReturnParameter));
        }
    }

    private static int buildMethodAccessModifier(PsiModifierList modifierList) {
        int methodAccess = 0;
        if (modifierList != null) {
            for (PsiElement child : modifierList.getChildren()) {
                switch (child.getText()) {
                    case "private":
                        methodAccess = methodAccess | Opcodes.ACC_PRIVATE;
                        break;
                    case "public":
                        methodAccess = methodAccess | Opcodes.ACC_PUBLIC;
                        break;
                    case "protected":
                        methodAccess = methodAccess | Opcodes.ACC_PROTECTED;
                        break;
                    case "static":
                        methodAccess = methodAccess | Opcodes.ACC_STATIC;
                        break;
                    case "final":
                        methodAccess = methodAccess | Opcodes.ACC_FINAL;
                        break;
                    default:
                        logger.warn("unhandled modifier: " + child.getText());
                }
            }
        }
        return methodAccess;
    }

    private List<TestCandidateMetadata> createBoilerplateTestCandidate(MethodAdapter methodAdapter1,
                                                                       boolean addFieldMocksCheckBox)
            throws ExecutionException, InterruptedException {

        if (methodAdapter1.getContainingClass() == null) {
            return new ArrayList<>();
        }
        ClassAdapter currentClass = methodAdapter1.getContainingClass();
        PsiClass parentOfType = currentClass.getSource();
        List<TestCandidateMetadata> testCandidateMetadataList = new ArrayList<>();

        HashMap<String, Parameter> fieldMapByName = new HashMap<>();
        VariableContainer fieldContainer = new VariableContainer();

        FieldAdapter[] fields = currentClass.getFields();
        for (FieldAdapter field : fields) {
            if (field.hasModifier(JvmModifier.STATIC)) {
                continue;
            }
            Parameter fieldParameter = new Parameter();
            String fieldName = field.getName();
            fieldParameter.setName(fieldName);

            PsiType fieldType = field.getType();

            String canonicalText = ApplicationManager.getApplication().runReadAction(
                    (Computable<String>) () -> {
                        TestCaseWriter.setParameterTypeFromPsiType(fieldParameter, fieldType, false);
                        return fieldType.getCanonicalText();
                    });
            if (!(fieldType instanceof PsiClassReferenceType)
                    || canonicalText.equals("java.lang.String")
                    || canonicalText.startsWith("org.apache.commons.logging")
                    || canonicalText.startsWith("org.slf4j")
            ) {
                continue;
            }

            fieldParameter.setValue(random.nextLong());
            fieldParameter.setProbeAndProbeInfo(new DataEventWithSessionId(), new DataInfo());
            fieldContainer.add(fieldParameter);
            fieldMapByName.put(fieldName, fieldParameter);
        }

        TestCandidateMetadata testCandidateMetadata = new TestCandidateMetadata();
        testCandidateMetadata.setLines(List.of());
        testCandidateMetadata.setCreatedAt(new Date().getTime());
        Parameter testSubjectParameter = new Parameter();
        testSubjectParameter.setType(currentClass.getQualifiedName());
        testSubjectParameter.setValue(random.nextLong());
        DataEventWithSessionId testSubjectParameterProbe = new DataEventWithSessionId();
        testSubjectParameter.setProbeAndProbeInfo(testSubjectParameterProbe, new DataInfo());

        List<TestCandidateMetadata> constructorCandidate = buildConstructorCandidate(currentClass, testSubjectParameter,
                fieldContainer);
        testCandidateMetadataList.addAll(constructorCandidate);

        Parameter returnValue = null;
        PsiType returnType1 = ApplicationManager.getApplication().runReadAction(
                (Computable<PsiType>) () -> methodAdapter1.getReturnType());
        if (returnType1 != null) {
            returnValue = new Parameter();
            returnValue.setValue(random.nextLong());

            TestCaseWriter.setParameterTypeFromPsiType(returnValue, returnType1, true);

            DataEventWithSessionId returnValueProbe = new DataEventWithSessionId();
            returnValue.setProbeAndProbeInfo(returnValueProbe, new DataInfo());
        } else if (methodAdapter1.isConstructor()) {
            returnValue = new Parameter();
            returnValue.setValue(random.nextLong());
            returnType1 = PsiTypesUtil.getClassType(methodAdapter1.getContainingClass().getSource());
            TestCaseWriter.setParameterTypeFromPsiType(returnValue,
                    returnType1, true);

            DataEventWithSessionId returnValueProbe = new DataEventWithSessionId();
            returnValue.setProbeAndProbeInfo(returnValueProbe, new DataInfo());
        }

        // method parameters
        ParameterAdapter[] parameterList = ApplicationManager.getApplication().runReadAction(
                (Computable<ParameterAdapter[]>) methodAdapter1::getParameters);
        List<Parameter> arguments = new ArrayList<>(parameterList.length);
        for (ParameterAdapter parameter : parameterList) {
            Parameter argumentParameter = new Parameter();

            argumentParameter.setValue(random.nextLong());

            PsiType parameterPsiType = ApplicationManager.getApplication().runReadAction(
                    (Computable<PsiType>) () -> {
                        TestCaseWriter.setParameterTypeFromPsiType(argumentParameter, parameter.getType(), false);
                        return parameter.getType();
                    });


            DataEventWithSessionId parameterProbe = new DataEventWithSessionId();
            argumentParameter.setProbeAndProbeInfo(parameterProbe, new DataInfo());
            String parameterName = parameter.getName();
            argumentParameter.setName(parameterName);

            if (argumentParameter.getType().equals("java.lang.String")) {
                argumentParameter.getProb().setSerializedValue(("\"" + parameterName + "\"").getBytes());
            } else if (argumentParameter.isPrimitiveType()) {
                argumentParameter.getProb().setSerializedValue(("0").getBytes());
            } else {
                argumentParameter.getProb().setSerializedValue(("{" + "}").getBytes());
            }

            arguments.add(argumentParameter);
        }


        PsiModifierList modifierList = ApplicationManager.getApplication().runReadAction(
                (Computable<PsiModifierList>) () -> methodAdapter1.getModifierList());

        int methodAccess = buildMethodAccessModifier(modifierList);
        MethodCallExpression mainMethod = new MethodCallExpression(
                ApplicationManager.getApplication().runReadAction((Computable<String>) () -> methodAdapter1.getName()),
                testSubjectParameter, arguments,
                returnValue, 0
        );
        mainMethod.setSubject(testSubjectParameter);
        mainMethod.setMethodAccess(methodAccess);

        testCandidateMetadata.setMainMethod(mainMethod);

        testCandidateMetadata.setTestSubject(testSubjectParameter);

        if (!methodAdapter1.isConstructor() && returnType1 != null && !returnType1.getCanonicalText().equals("void")) {
            Parameter assertionExpectedValue = new Parameter();
            assertionExpectedValue.setName(returnValue.getName() + "Expected");
            assertionExpectedValue.setProbeAndProbeInfo(new DataEventWithSessionId(), new DataInfo());

            TestCaseWriter.setParameterTypeFromPsiType(assertionExpectedValue, returnType1, true);
            TestAssertion testAssertion = new TestAssertion(AssertionType.EQUAL, assertionExpectedValue, returnValue);
            testCandidateMetadata.getAssertionList().add(testAssertion);
        }

        // fields
        if (addFieldMocksCheckBox) {
            // field parameters are going to be mocked and then injected
            fieldContainer.all().forEach(e -> testCandidateMetadata.getFields().add(e));
        }

//        methodChecked = new ArrayList<>();
        List<MethodCallExpression> collectedMceList = ApplicationManager.getApplication().runReadAction(
                (Computable<List<MethodCallExpression>>) () -> extractMethodCalls(methodAdapter1, fieldMapByName));
        for (int i = 0; i < collectedMceList.size(); i++) {
            MethodCallExpression methodCallExpression = collectedMceList.get(i);
            DataEventWithSessionId entryProbe = new DataEventWithSessionId();
            entryProbe.setEventId(i);
            methodCallExpression.setEntryProbe(entryProbe);
        }

        testCandidateMetadata.getCallsList().addAll(collectedMceList);
        testCandidateMetadataList.add(testCandidateMetadata);
        return testCandidateMetadataList;
    }

    public List<PsiMethodCallExpression> collectMethodCallExpressions(PsiElement element) {
        ArrayList<PsiMethodCallExpression> returnList = new ArrayList<>();
        if (element == null) {
            return returnList;
        }
        if (element instanceof PsiMethodCallExpression) {
            returnList.add((PsiMethodCallExpression) element);
        }
        PsiElement[] children = element.getChildren();

        for (PsiElement child : children) {
            returnList.addAll(collectMethodCallExpressions(child));
        }
        return returnList;
    }


    private List<MethodCallExpression> extractMethodCalls(MethodAdapter methodAdapter,
                                                          Map<String, Parameter> fieldMapByName) {
        List<MethodCallExpression> collectedMceList = new ArrayList<>();
        Map<Object, Boolean> valueUsed = new HashMap<>();

        List<PsiMethodCallExpression> mceList = collectMethodCallExpressions(methodAdapter.getBody());
        for (PsiMethodCallExpression psiMethodCallExpression : mceList) {
            if (psiMethodCallExpression == null) {
                continue;
            }
            PsiElement[] callExpressionChildren = psiMethodCallExpression.getChildren();
            if (callExpressionChildren[0] instanceof PsiReferenceExpressionImpl) {
                PsiReferenceExpressionImpl callReferenceExpression = (PsiReferenceExpressionImpl) callExpressionChildren[0];
                PsiExpressionListImpl callParameterExpression = null;
                if (callExpressionChildren[1] instanceof PsiExpressionListImpl) {
                    callParameterExpression = (PsiExpressionListImpl) callExpressionChildren[1];
                } else if (callExpressionChildren[2] instanceof PsiExpressionListImpl) {
                    callParameterExpression = (PsiExpressionListImpl) callExpressionChildren[2];
                }
                if (callParameterExpression == null) {
                    logger.warn("failed to extract call from call expression: " + psiMethodCallExpression.getText());
                    continue;
                }

                PsiElement[] referenceChildren = callReferenceExpression.getChildren();
                if (referenceChildren.length == 4) {

                    // <fieldName><dot><methodTemplateParams(implicit, mostly empty)><methodName>

                    PsiElement subjectReferenceChild = referenceChildren[0];
                    PsiElement dotChild = referenceChildren[1];
                    PsiElement paramChild = referenceChildren[2];
                    PsiElement methodNameNode = referenceChildren[3];
                    if (!(dotChild instanceof PsiJavaTokenImpl) || !dotChild.getText().equals(".")) {
                        // second child was supposed to be a dot
                        continue;
                    }

                    if (subjectReferenceChild instanceof PsiThisExpressionImpl) {
                        String invokedMethodName = methodNameNode.getText();
//                        if (methodChecked.contains(invokedMethodName)) {
//                            continue;
//                        }
//                        methodChecked.add(invokedMethodName);
                        List<MethodCallExpression> callExpressions = getCallsFromMethod(methodAdapter,
                                callParameterExpression, invokedMethodName, fieldMapByName);
                        collectedMceList.addAll(callExpressions);
                    } else {

                        String fieldName = subjectReferenceChild.getText();
                        Parameter fieldByName = fieldMapByName.get(fieldName);
                        if (fieldByName == null) {
                            // no such field
                            continue;
                        }

                        List<Parameter> methodArguments = new ArrayList<>();
                        Parameter methodReturnValue = new Parameter();
                        DataInfo probeInfo1 = new DataInfo(
                                0, 0, 0, 0, 0, EventType.ARRAY_LENGTH, Descriptor.Boolean, ""
                        );

                        methodReturnValue.setProbeAndProbeInfo(new DataEventWithSessionId(), probeInfo1);

                        ClassAdapter calledMethodClassReference = getClassByName(fieldByName.getType(),
                                methodAdapter.getProject());
                        if (calledMethodClassReference == null) {
                            logger.warn(
                                    "Class reference[" + fieldByName.getType() + "] for methodAdapter call expression " +
                                            "not found [" + psiMethodCallExpression.getMethodExpression() + "]");
                            continue;
                        }


                        String methodName = methodNameNode.getText();
                        MethodAdapter matchedMethod = getMatchingMethod(calledMethodClassReference, methodName,
                                callParameterExpression);
                        if (matchedMethod == null) {
                            logger.warn("could not resolve reference to methodAdapter: [" +
                                    methodName + "] in class: " + fieldByName.getType());
                            continue;
                        }

                        PsiExpression[] actualParameterExpressions = callParameterExpression.getExpressions();
                        ParameterAdapter[] parameters = matchedMethod.getParameters();

                        PsiClass parentOfType = PsiTreeUtil.getParentOfType(callParameterExpression, PsiClass.class);

                        PsiField callOnField = null;
                        for (PsiField field : parentOfType.getFields()) {
                            if (field.getName().equals(fieldName)) {
                                callOnField = field;
                                break;
                            }
                        }

                        PsiSubstitutor classSubstitutor = ClassUtils.getSubstitutorForCallExpression(
                                psiMethodCallExpression);
                        PsiType fieldType = ClassTypeUtils.substituteClassRecursively(callOnField.getType(),
                                classSubstitutor);

                        TestCaseWriter.setParameterTypeFromPsiType(fieldByName, fieldType, false);

                        for (int i = 0; i < parameters.length; i++) {
                            ParameterAdapter parameter = parameters[i];
                            PsiExpression parameterExpression = actualParameterExpressions[i];

                            Parameter callParameter = new Parameter();
                            PsiType typeToAssignFrom = parameterExpression.getType();


                            if (typeToAssignFrom == null || typeToAssignFrom.getCanonicalText().equals("null")) {
                                typeToAssignFrom = parameter.getType();
                            }
                            final PsiType ungenericType = ClassTypeUtils.substituteClassRecursively(typeToAssignFrom,
                                    classSubstitutor);

                            TestCaseWriter.setParameterTypeFromPsiType(callParameter, ungenericType, false);


                            long nextValue;

                            if (!(typeToAssignFrom instanceof PsiPrimitiveType)) {
                                nextValue = random.nextLong();
                            } else {
                                PsiPrimitiveType primitiveType = ((PsiPrimitiveType) typeToAssignFrom);
                                switch (primitiveType.getName()) {
                                    case "int":
                                        nextValue = random.nextInt();
                                        break;
                                    case "long":
                                        nextValue = random.nextLong();
                                        break;
                                    case "short":
                                        nextValue = random.nextInt();
                                        break;
                                    case "byte":
                                        nextValue = random.nextInt();
                                        break;
                                    case "boolean":
                                        nextValue = random.nextBoolean() ? 1L : 0L;
                                        break;
                                    case "float":
                                        nextValue = Float.floatToIntBits(random.nextFloat());
                                        break;
                                    case "double":
                                        nextValue = Double.doubleToLongBits(random.nextDouble());
                                        break;
                                    default:
                                        nextValue = random.nextInt();
                                        break;
                                }
                            }
                            valueUsed.put(nextValue, true);

                            callParameter.setValue(nextValue);
                            DataEventWithSessionId prob = new DataEventWithSessionId();
                            if (callParameter.isPrimitiveType()) {
                                prob.setSerializedValue("0".getBytes());
                            } else if (callParameter.isStringType()) {
                                prob.setSerializedValue(("\"" + parameter.getName() + "\"").getBytes());
                            } else {
                                String serializedStringValue = ClassUtils.createDummyValue(
                                        typeToAssignFrom, new LinkedList<>(),
                                        methodAdapter.getProject()
                                );
                                prob.setSerializedValue(serializedStringValue.getBytes());

                            }
                            callParameter.setName(parameter.getName());
                            DataInfo probeInfo = new DataInfo(
                                    0, 0, 0, 0, 0, EventType.ARRAY_LENGTH, Descriptor.Boolean, ""
                            );
                            callParameter.setProbeAndProbeInfo(prob, probeInfo);
                            methodArguments.add(callParameter);
                        }

                        PsiType ungenericReturnClassType = ClassTypeUtils.substituteClassRecursively(
                                matchedMethod.getReturnType(),
                                classSubstitutor);

                        methodReturnValue.setValue(random.nextLong());
                        TestCaseWriter.setParameterTypeFromPsiType(methodReturnValue, ungenericReturnClassType, true);
                        DataInfo probeInfo = new DataInfo(
                                0, 0, 0, 0, 0, EventType.ARRAY_LENGTH, Descriptor.Boolean, ""
                        );
                        DataEventWithSessionId returnValueDataEvent = new DataEventWithSessionId();

                        String dummyValue = ClassUtils.createDummyValue(ungenericReturnClassType,
                                new LinkedList<>(), methodAdapter.getProject());


                        returnValueDataEvent.setSerializedValue(dummyValue.getBytes());
                        methodReturnValue.setProbeAndProbeInfo(returnValueDataEvent, probeInfo);


                        MethodCallExpression mce = new MethodCallExpression(
                                methodName, fieldByName, methodArguments, methodReturnValue, 0
                        );
                        int methodAccess = buildMethodAccessModifier(matchedMethod.getModifierList());
                        methodAccess = methodAccess | Opcodes.ACC_PUBLIC;
                        mce.setMethodAccess(methodAccess);

                        collectedMceList.add(mce);
                    }
                } else if (referenceChildren.length == 2) {
                    // call to a methodAdapter in same class
                    PsiElement methodNameNode = referenceChildren[1];
                    String invokedMethodName = methodNameNode.getText();
                    List<MethodCallExpression> callExpressions = getCallsFromMethod(methodAdapter,
                            callParameterExpression,
                            invokedMethodName, fieldMapByName);
                    collectedMceList.addAll(callExpressions);
                }


            } else {
                logger.error("unknown type of child: " + callExpressionChildren[0]);
            }
        }
        return collectedMceList;
    }

    private List<MethodCallExpression> getCallsFromMethod(MethodAdapter methodAdapter,
                                                          PsiExpressionListImpl callParameterExpression,
                                                          String invokedMethodName, Map<String, Parameter> fieldMapByName) {
        MethodAdapter matchedMethod = getMatchingMethod(methodAdapter.getContainingClass(), invokedMethodName,
                callParameterExpression);
        if (matchedMethod == null) {
            return Collections.emptyList();
        }

        return extractMethodCalls(matchedMethod, fieldMapByName);
    }

    private Set<ClassAdapter> getInterfaces(ClassAdapter psiClass) {
        Set<ClassAdapter> interfacesList = new HashSet<>();
        ClassAdapter[] interfaces = psiClass.getInterfaces();
        for (ClassAdapter anInterface : interfaces) {
            interfacesList.add(anInterface);
            interfacesList.addAll(getInterfaces(anInterface));
        }
        ClassAdapter[] supers = psiClass.getSupers();
        for (ClassAdapter aSuper : supers) {
            interfacesList.add(aSuper);
            interfacesList.addAll(getInterfaces(aSuper));
        }
        return interfacesList;
    }


    private MethodAdapter getMatchingMethod(
            ClassAdapter classReference,
            String methodName,
            PsiExpressionListImpl callParameterExpression
    ) {
        logger.debug("Find matching method for [" + methodName + "] - " + classReference.getName());
        List<ClassAdapter> classesToCheck = new ArrayList<>();
        classesToCheck.add(classReference);
        Set<ClassAdapter> interfaces = getInterfaces(classReference);
        classesToCheck.addAll(interfaces);

        for (ClassAdapter psiClass : classesToCheck) {
            MethodAdapter[] methods = psiClass.getMethods();
            List<MethodAdapter> matchedMethods = Arrays.stream(methods)
                    .filter(e -> e.getName().equals(methodName))
                    .filter(e -> e.getParameters().length == callParameterExpression.getExpressionCount())
                    .collect(Collectors.toList());
            if (matchedMethods.isEmpty()) {
                continue;
            }
            for (MethodAdapter matchedMethod : matchedMethods) {
                boolean isMismatch = false;
                logger.debug(
                        "Check matched method: [" + matchedMethod + "] in class [" + psiClass.getQualifiedName() + "]");
                PsiType[] expectedExpressionType = callParameterExpression.getExpressionTypes();
                ParameterAdapter[] parameters = matchedMethod.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    ParameterAdapter parameter = parameters[i];
                    PsiType type = parameter.getType();
                    if (type instanceof PsiClassReferenceType && type.getCanonicalText().length() == 1) {
                        // this is a generic type, wont match with the actual expression type
                        continue;
                    }
                    if (expectedExpressionType[i] == null || !type.isAssignableFrom(expectedExpressionType[i])) {
                        logger.warn("parameter [" + i + "] mismatch [" + type + " vs " + expectedExpressionType[i]);
                        isMismatch = true;
                        break;
                    }
                }
                if (!isMismatch) {
                    return matchedMethod;
                }
            }
        }
        return null;
    }


    private List<TestCandidateMetadata> buildConstructorCandidate(
            ClassAdapter currentClass, Parameter testSubject, VariableContainer fieldContainer) {
        List<TestCandidateMetadata> candidateList = new ArrayList<>();

        MethodAdapter[] constructors = currentClass.getConstructors();
        if (constructors.length == 0) {
            TestCandidateMetadata newTestCaseMetadata = new TestCandidateMetadata();
            MethodCallExpression constructorMethod = new MethodCallExpression("<init>", testSubject,
                    Collections.emptyList(), testSubject, 0);
            constructorMethod.setMethodAccess(1);
            newTestCaseMetadata.setMainMethod(constructorMethod);
            newTestCaseMetadata.setTestSubject(testSubject);
            newTestCaseMetadata.setFields(VariableContainer.from(Collections.emptyList()));
            candidateList.add(newTestCaseMetadata);

            return candidateList;
        }
        MethodAdapter selectedConstructor = null;
        for (MethodAdapter constructor : constructors) {
            if (selectedConstructor == null) {
                selectedConstructor = constructor;
                continue;
            }
            ParameterAdapter[] constructorParameterList = constructor.getParameters();
            ParameterAdapter[] selectedConstructorParameterList = selectedConstructor.getParameters();
            if (constructorParameterList.length > selectedConstructorParameterList.length) {
                boolean isRecursiveConstructor = false;
                for (ParameterAdapter parameter : constructorParameterList) {
                    PsiType type = parameter.getType();
                    if (currentClass.getQualifiedName().equals(type.getCanonicalText())) {
                        isRecursiveConstructor = true;
                    }
                }
                if (!isRecursiveConstructor) {
                    selectedConstructor = constructor;
                }
            }
        }

        if (selectedConstructor == null) {
            logger.error("selectedConstructor should not have been null: " + currentClass.getQualifiedName());
            return candidateList;
        }

        logger.warn("selected constructor for [" + currentClass.getQualifiedName()
                + "] -> " + selectedConstructor.getName());


        TestCandidateMetadata candidate = new TestCandidateMetadata();
        List<Parameter> methodArguments = new ArrayList<>(selectedConstructor.getParameters().length);

        for (ParameterAdapter parameter : selectedConstructor.getParameters()) {

            PsiType parameterType = parameter.getType();
            List<Parameter> fieldParameterByType = fieldContainer
                    .getParametersByType(parameterType.getCanonicalText());
            String parameterName = parameter.getName();
//            if (fieldParameterByType.isEmpty()) {
//                Parameter fieldParamByName = fieldContainer.getParameterByName(parameterName);
//                if (fieldParamByName != null) {
//                    fieldParameterByType.add(fieldParamByName);
//                }
//            }

            if (!fieldParameterByType.isEmpty()) {

                Parameter closestNameMatch = fieldParameterByType.get(0);
                int currentDistance = Integer.MAX_VALUE;

                for (Parameter fieldParameter : fieldParameterByType) {
                    int distance = StringUtils.getLevenshteinDistance(fieldParameter.getName(), parameterName);
                    if (distance < currentDistance) {
                        closestNameMatch = fieldParameter;
                        currentDistance = distance;
                    }
                }
                methodArguments.add(closestNameMatch);

            } else {
                Parameter methodArgumentParameter = new Parameter();
                methodArgumentParameter.setName(parameterName);
                TestCaseWriter.setParameterTypeFromPsiType(methodArgumentParameter, parameterType, false);
                methodArgumentParameter.setValue(random.nextLong());
                DataEventWithSessionId argumentProbe = new DataEventWithSessionId();

                if (methodArgumentParameter.isPrimitiveType()) {
                    argumentProbe.setSerializedValue("0".getBytes());
                } else if (methodArgumentParameter.isStringType()) {
                    argumentProbe.setSerializedValue("\"\"".getBytes());
                } else {

                    String parameterClassName = parameterType.getCanonicalText();
                    if (parameterType instanceof PsiClassReferenceType) {
                        parameterClassName = ((PsiClassReferenceType) parameterType).rawType().getCanonicalText();
                    }
                    ClassAdapter parameterClassReference = getClassByName(parameterClassName,
                            currentClass.getProject());

                    if (parameterClassReference == null) {
                        logger.warn("did not find class reference: " + parameterClassName +
                                " for parameter: " + parameterName +
                                " in class " + currentClass.getQualifiedName());
                        continue;
                    }

                    if (parameterClassReference.getQualifiedName().equals(currentClass.getQualifiedName())) {
                        continue;
                    }

                    List<TestCandidateMetadata> constructorMetadata =
                            buildConstructorCandidate(parameterClassReference, methodArgumentParameter, fieldContainer);
                    candidateList.addAll(constructorMetadata);
                }
                methodArgumentParameter.setProbeAndProbeInfo(argumentProbe, new DataInfo());
                methodArguments.add(methodArgumentParameter);
            }
        }

        MethodCallExpression constructorMethod = new MethodCallExpression("<init>", testSubject, methodArguments,
                testSubject, 0);
        constructorMethod.setMethodAccess(Opcodes.ACC_PUBLIC);

        candidate.setMainMethod(constructorMethod);
        candidate.setTestSubject(testSubject);
        candidateList.add(0, candidate);
        return candidateList;
    }


    private ClassAdapter getClassByName(String className, Project project) {
        PsiClass aClass = JavaPsiFacade.getInstance(project)
                .findClass(ClassTypeUtils.getDescriptorToDottedClassName(className),
                        GlobalSearchScope.allScope(project));
        if (aClass == null) {
            return null;
        }
        return new JavaClassAdapter(aClass);
    }


    public List<TestCandidateMetadata> generateTestCaseBoilerPlace(MethodAdapter methodAdapter1,
                                                                   TestCaseGenerationConfiguration generationConfiguration) {

        PsiFile containingFile = methodAdapter1.getContainingFile();
        if (containingFile.getVirtualFile() == null || containingFile.getVirtualFile().getPath().contains("/test/")) {
            InsidiousNotification.notifyMessage("Failed to get method source code", NotificationType.ERROR);
            return null;
        }

        List<TestCandidateMetadata> testCandidateMetadataList = null;
        try {
            testCandidateMetadataList = ApplicationManager.getApplication()
                    .executeOnPooledThread(() -> ApplicationManager.getApplication().runReadAction(
                            (Computable<List<TestCandidateMetadata>>) () -> {
                                try {
                                    return createBoilerplateTestCandidate(methodAdapter1,
                                            generationConfiguration.isAddFieldMocksCheckBox());
                                } catch (ExecutionException | InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            })).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        if (testCandidateMetadataList == null) {
            InsidiousNotification.notifyMessage("Failed to create test case boilerplate", NotificationType.ERROR);
            return null;

        }


        return testCandidateMetadataList;
    }

}
