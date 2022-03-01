package com.insidious.plugin.extension.evaluation;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.DebuggerManager;
import com.intellij.debugger.NoDataException;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.ide.util.JavaAnonymousClassesHelper;
import com.intellij.openapi.application.ReadAction;
import org.slf4j.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.sun.jdi.ReferenceType;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.thread.InsidiousVirtualMachineProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class JVMNameUtil {
    public static final String CONSTRUCTOR_NAME = "<init>";
    private static final Logger LOG = LoggerUtil.getInstance(JVMNameUtil.class);

    @Nullable
    public static String getPrimitiveSignature(String typeName) {
        if (PsiType.BOOLEAN.getCanonicalText().equals(typeName))
            return "Z";
        if (PsiType.BYTE.getCanonicalText().equals(typeName))
            return "B";
        if (PsiType.CHAR.getCanonicalText().equals(typeName))
            return "C";
        if (PsiType.SHORT.getCanonicalText().equals(typeName))
            return "S";
        if (PsiType.INT.getCanonicalText().equals(typeName))
            return "I";
        if (PsiType.LONG.getCanonicalText().equals(typeName))
            return "J";
        if (PsiType.FLOAT.getCanonicalText().equals(typeName))
            return "F";
        if (PsiType.DOUBLE.getCanonicalText().equals(typeName))
            return "D";
        if (PsiType.VOID.getCanonicalText().equals(typeName)) {
            return "V";
        }
        return null;
    }


    private static void appendJVMSignature(JVMNameBuffer buffer, PsiType type) {
        if (type == null) {
            return;
        }
        PsiType psiType = TypeConversionUtil.erasure(type);
        if (psiType instanceof PsiArrayType) {
            buffer.append(new JVMRawText("["));
            appendJVMSignature(buffer, ((PsiArrayType) psiType).getComponentType());
        } else if (psiType instanceof com.intellij.psi.PsiClassType) {
            JVMName jvmName = getJVMQualifiedName(psiType);
            appendJvmClassQualifiedName(buffer, jvmName);
        } else if (psiType instanceof com.intellij.psi.PsiPrimitiveType) {
            buffer.append(getPrimitiveSignature(psiType.getCanonicalText()));
        } else {
            LOG.error("unknown type " + type.getCanonicalText());
        }
    }


    private static void appendJvmClassQualifiedName(JVMNameBuffer buffer, final JVMName jvmName) {
        buffer.append("L");
        if (jvmName instanceof JVMRawText) {
            buffer.append(((JVMRawText) jvmName).getName().replace('.', '/'));
        } else {
            buffer.append(new JVMName() {

                public String getName(InsidiousVirtualMachineProxy proxy) throws EvaluateException {
                    return jvmName.getName(proxy).replace('.', '/');
                }


                public String getDisplayName(InsidiousVirtualMachineProxy proxy) {
                    return jvmName.getDisplayName(proxy);
                }
            });
        }
        buffer.append(";");
    }

    @NotNull
    public static JVMName getJVMRawText(String qualifiedName) {
        return new JVMRawText(qualifiedName);
    }

    public static JVMName getJVMQualifiedName(PsiType psiType) {
        if (psiType instanceof PsiArrayType) {
            PsiArrayType arrayType = (PsiArrayType) psiType;
            JVMName jvmName = getJVMQualifiedName(arrayType.getComponentType());
            JVMNameBuffer buffer = new JVMNameBuffer();
            buffer.append(jvmName);
            buffer.append("[]");
            return buffer.toName();
        }

        PsiClass psiClass = PsiUtil.resolveClassInType(psiType);
        if (psiClass == null) {
            return getJVMRawText(psiType.getCanonicalText());
        }
        return getJVMQualifiedName(psiClass);
    }

    @NotNull
    public static JVMName getJVMQualifiedName(@NotNull PsiClass psiClass) {
        String name = getClassVMName(psiClass);
        if (name != null) {
            return getJVMRawText(name);
        }
        return new JVMClassAt(SourcePosition.createFromElement(psiClass));
    }

    @Nullable
    public static JVMName getContextClassJVMQualifiedName(@Nullable SourcePosition pos) {
        PsiClass psiClass = getClassAt(pos);
        if (psiClass == null) {
            return null;
        }
        String name = getNonAnonymousClassName(psiClass);
        if (name != null) {
            return getJVMRawText(name);
        }
        return new JVMClassAt(pos);
    }

    @Nullable
    public static String getNonAnonymousClassName(@NotNull PsiClass aClass) {
        if (PsiUtil.isLocalOrAnonymousClass(aClass)) {
            return null;
        }
        String name = aClass.getName();
        if (name == null) {
            return null;
        }
        PsiClass parentClass = PsiTreeUtil.getParentOfType(aClass, PsiClass.class, true);
        if (parentClass != null) {
            String parentName = getNonAnonymousClassName(parentClass);
            if (parentName == null) {
                return null;
            }
            return parentName + "$" + name;
        }
        return DebuggerManager.getInstance(aClass.getProject()).getVMClassQualifiedName(aClass);
    }

    @NotNull
    public static JVMName getJVMConstructorSignature(@Nullable PsiMethod method, @Nullable PsiClass declaringClass) {
        return getJVMSignature(method, true, declaringClass);
    }

    @NotNull
    public static JVMName getJVMSignature(@NotNull PsiMethod method) {
        return getJVMSignature(method, method.isConstructor(), method.getContainingClass());
    }

    @NotNull
    public static String getJVMMethodName(@NotNull PsiMethod method) {
        return method.isConstructor() ? "<init>" : method.getName();
    }

    @NotNull
    private static JVMName getJVMSignature(@Nullable PsiMethod method, boolean constructor, @Nullable PsiClass declaringClass) {
        JVMNameBuffer signature = new JVMNameBuffer();
        signature.append("(");

        if (constructor &&
                declaringClass != null) {
            PsiClass outerClass = declaringClass.getContainingClass();
            if (outerClass != null) {
                if (!declaringClass.hasModifierProperty("static")) {
                    appendJvmClassQualifiedName(signature, getJVMQualifiedName(outerClass));
                }
            }
        }

        if (method != null) {
            for (PsiParameter psiParameter : method.getParameterList().getParameters()) {
                appendJVMSignature(signature, psiParameter.getType());
            }
        }
        signature.append(")");
        if (!constructor && method != null) {
            appendJVMSignature(signature, method.getReturnType());
        } else {
            signature.append(new JVMRawText("V"));
        }
        return signature.toName();
    }

    @Nullable
    public static PsiClass getClassAt(@Nullable SourcePosition position) {
        if (position == null) {
            return null;
        }
        PsiElement element = position.getElementAt();
        return (element != null && element.isValid()) ?
                PsiTreeUtil.getParentOfType(element, PsiClass.class, false) :
                null;
    }

    @Nullable
    public static String getSourcePositionClassDisplayName(InsidiousVirtualMachineProxy proxy, @Nullable SourcePosition position) {
        if (position == null) {
            return null;
        }


        Pair<String, Boolean> res = ReadAction.compute(() -> {
            PsiFile positionFile = position.getFile();


            if (positionFile instanceof com.intellij.psi.jsp.JspFile) {
                return Pair.create(positionFile.getName(), Boolean.valueOf(false));
            }


            PsiClass psiClass = getClassAt(position);


            if (psiClass != null) {
                String qName = psiClass.getQualifiedName();


                if (qName != null) {
                    return Pair.create(qName, Boolean.valueOf(false));
                }
            }


            return (psiClass == null) ? ((positionFile instanceof PsiClassOwner) ? Pair.create(positionFile.getName(), Boolean.valueOf(true)) : Pair.create(DebuggerBundle.message("string.file.line.position", positionFile.getName(), Integer.valueOf(position.getLine())), Boolean.valueOf(true))) : Pair.create(calcClassDisplayName(psiClass), Boolean.valueOf(true));
        });


        if (res.second.booleanValue() && proxy != null && proxy.isAttached()) {
            try {
                List<ReferenceType> allClasses = proxy.getPositionManager().getAllClasses(position);
                if (!allClasses.isEmpty()) {
                    return allClasses.get(0).name();
                }
            } catch (NoDataException noDataException) {
            }
        }

        return res.first;
    }

    static String calcClassDisplayName(final PsiClass aClass) {
        String qName = aClass.getQualifiedName();
        if (qName != null) {
            return qName;
        }
        PsiClass parent = PsiTreeUtil.getParentOfType(aClass, PsiClass.class, true);
        if (parent == null) {
            return null;
        }

        String name = aClass.getName();
        if (name != null) {
            return calcClassDisplayName(parent) + "$" + name;
        }

        final Ref<Integer> classIndex = new Ref(Integer.valueOf(0));
        try {
            parent.accept(new JavaRecursiveElementVisitor() {
                public void visitAnonymousClass(PsiAnonymousClass cls) {
                    classIndex.set(Integer.valueOf(classIndex.get().intValue() + 1));
                    if (aClass.equals(cls)) {
                        throw new ProcessCanceledException();
                    }
                }
            });
        } catch (ProcessCanceledException processCanceledException) {
        }

        return calcClassDisplayName(parent) + "$" + classIndex.get();
    }

    @Nullable
    public static String getSourcePositionPackageDisplayName(DebugProcessImpl debugProcess, @Nullable SourcePosition position) {
        if (position == null) {
            return null;
        }


        String res = ReadAction.compute(() -> {
            PsiFile positionFile = position.getFile();

            if (positionFile instanceof com.intellij.psi.jsp.JspFile) {
                PsiDirectory dir = positionFile.getContainingDirectory();

                return (dir != null) ? dir.getVirtualFile().getPresentableUrl() : null;
            }

            PsiClass psiClass = getClassAt(position);

            if (psiClass != null) {
                PsiClass toplevel = PsiUtil.getTopLevelClass(psiClass);

                if (toplevel != null) {
                    String qName = toplevel.getQualifiedName();

                    if (qName != null) {
                        int i = qName.lastIndexOf('.');

                        return (i > 0) ? qName.substring(0, i) : "";
                    }
                }
            }
            if (positionFile instanceof PsiClassOwner) {
                String name = ((PsiClassOwner) positionFile).getPackageName();
                if (!StringUtil.isEmpty(name)) {
                    return name;
                }
            }
            return null;
        });
        if (res == null && debugProcess != null && debugProcess.isAttached()) {

            List<ReferenceType> allClasses = debugProcess.getPositionManager().getAllClasses(position);
            if (!allClasses.isEmpty()) {
                String className = allClasses.get(0).name();
                int dotIndex = className.lastIndexOf('.');
                if (dotIndex >= 0) {
                    return className.substring(0, dotIndex);
                }
            }
        }
        return "";
    }

    public static PsiClass getTopLevelParentClass(PsiClass psiClass) {
        return PsiTreeUtil.getTopmostParentOfType(psiClass, PsiClass.class);
    }

    @Nullable
    public static String getClassVMName(@Nullable PsiClass containingClass) {
        if (containingClass == null) return null;
        if (containingClass instanceof PsiAnonymousClass) {

            String parentName = getClassVMName(PsiTreeUtil.getParentOfType(containingClass, PsiClass.class));
            if (parentName == null) {
                return null;
            }
            return parentName +
                    JavaAnonymousClassesHelper.getName((PsiAnonymousClass) containingClass);
        }

        return getNonAnonymousClassName(containingClass);
    }

    private static class JVMNameBuffer {
        private final List<JVMName> myList = new ArrayList<>();

        private JVMNameBuffer() {
        }

        public void append(@NotNull JVMName evaluator) {
            this.myList.add(evaluator);
        }

        public void append(char name) {
            append(Character.toString(name));
        }

        public void append(String text) {
            this.myList.add(JVMNameUtil.getJVMRawText(text));
        }

        public JVMName toName() {
            final List<JVMName> optimised = new ArrayList<>();
            for (JVMName evaluator : this.myList) {
                if (evaluator instanceof JVMRawText &&
                        !optimised.isEmpty() && optimised
                        .get(optimised.size() - 1) instanceof JVMRawText) {

                    JVMRawText nameEvaluator = (JVMRawText) optimised.get(optimised.size() - 1);
                    nameEvaluator.setName(nameEvaluator
                            .getName() + ((JVMRawText) evaluator)
                            .getName());
                    continue;
                }
                optimised.add(evaluator);
            }


            if (optimised.size() == 1) return optimised.get(0);
            if (optimised.isEmpty()) return new JVMRawText("");

            return new JVMName() {
                String myName = null;


                public String getName(InsidiousVirtualMachineProxy proxy) throws EvaluateException {
                    if (this.myName == null) {
                        String name = "";
                        for (JVMName nameEvaluator : optimised) {
                            name = name + nameEvaluator.getName(proxy);
                        }
                        this.myName = name;
                    }
                    return this.myName;
                }


                public String getDisplayName(InsidiousVirtualMachineProxy proxy) {
                    if (this.myName == null) {
                        String displayName = "";
                        for (JVMName nameEvaluator : optimised) {
                            displayName = displayName + nameEvaluator.getDisplayName(proxy);
                        }
                        return displayName;
                    }
                    return this.myName;
                }
            };
        }
    }

    private static class JVMRawText implements JVMName {
        private String myText;

        JVMRawText(String text) {
            this.myText = text;
        }


        public String getName(InsidiousVirtualMachineProxy proxy) throws EvaluateException {
            return this.myText;
        }


        public String getDisplayName(InsidiousVirtualMachineProxy proxy) {
            return this.myText;
        }

        public String getName() {
            return this.myText;
        }

        public void setName(String name) {
            this.myText = name;
        }


        public String toString() {
            return this.myText;
        }
    }

    private static class JVMClassAt implements JVMName {
        private final SourcePosition mySourcePosition;

        JVMClassAt(SourcePosition sourcePosition) {
            this.mySourcePosition = sourcePosition;
        }


        public String getName(InsidiousVirtualMachineProxy proxy) throws EvaluateException {
            try {
                List<ReferenceType> allClasses = proxy.getPositionManager().getAllClasses(this.mySourcePosition);

                if (allClasses.size() > 1) {

                    String name = ReadAction.compute(() -> JVMNameUtil.getClassVMName(JVMNameUtil.getClassAt(this.mySourcePosition)));
                    for (ReferenceType aClass : allClasses) {
                        if (Objects.equals(aClass.name(), name)) {
                            return name;
                        }
                    }
                }
                if (!allClasses.isEmpty()) {
                    return allClasses.get(0).name();
                }
            } catch (NoDataException e) {
                throw new EvaluateException(e.getMessage(), e);
            }

            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("error.class.not.loaded", getDisplayName(proxy)));
        }


        public String getDisplayName(InsidiousVirtualMachineProxy proxy) {
            return JVMNameUtil.getSourcePositionClassDisplayName(proxy, this.mySourcePosition);
        }
    }
}

