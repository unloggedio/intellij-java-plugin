package com.insidious.plugin.extension;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.MultiRequestPositionManager;
import com.intellij.debugger.NoDataException;
import com.intellij.debugger.PositionManager;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.JVMNameUtil;
import com.intellij.debugger.engine.PositionManagerImpl;
import com.intellij.debugger.impl.AlternativeJreClassFinder;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import org.slf4j.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.xdebugger.XDebuggerUtil;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.ClassPrepareRequest;
import com.insidious.plugin.extension.util.DebuggerUtil;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class InsidiousPositionManager implements PositionManager, MultiRequestPositionManager {
    private static final Logger logger = LoggerUtil.getInstance(InsidiousPositionManager.class);

    private final InsidiousJavaDebugProcess myDebugProcess;

    public InsidiousPositionManager(InsidiousJavaDebugProcess debugProcess) {
        this.myDebugProcess = debugProcess;
    }

    private static Set<PsiClass> getLineClasses(PsiFile file, int lineNumber) {
        ApplicationManager.getApplication().assertReadAccessAllowed();
        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        Set<PsiClass> res = new HashSet<>();
        if (document != null) {
            XDebuggerUtil.getInstance()
                    .iterateLine(file
                            .getProject(), document, lineNumber, element -> {
                        PsiClass aClass = getEnclosingClass(element);

                        if (aClass != null) {
                            res.add(aClass);
                        }

                        return true;
                    });
        }

        return res;
    }

    @Nullable
    public static PsiClass findClass(Project project, String originalQName, GlobalSearchScope searchScope, boolean fallbackToAllScope) {
        PsiClass psiClass = DebuggerUtils.findClass(originalQName, project, searchScope);

        if (psiClass == null) {
            int dollar = originalQName.indexOf('$');
            if (dollar > 0) {
                psiClass = DebuggerUtils.findClass(originalQName
                        .substring(0, dollar), project, searchScope);
            }
        }
        return psiClass;
    }

    private static Pair<PsiClass, Integer> getTopOrStaticEnclosingClass(PsiClass aClass) {
        int depth = 0;
        PsiClass enclosing = getEnclosingClass(aClass);
        while (enclosing != null) {
            depth++;
            if (enclosing.hasModifierProperty("static")) {
                break;
            }
            PsiClass next = getEnclosingClass(enclosing);
            if (next == null) {
                break;
            }
            enclosing = next;
        }
        return Pair.create(enclosing, Integer.valueOf(depth));
    }

    @Nullable
    private static PsiClass getEnclosingClass(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }

        element = element.getParent();
        PsiElement previous = null;

        while (element != null) {
            if (element instanceof PsiClass && !(previous instanceof com.intellij.psi.PsiExpressionList)) {
                return (PsiClass) element;
            }
            if (element instanceof PsiFile) {
                return null;
            }
            previous = element;
            element = element.getParent();
        }

        return null;
    }

    @Nullable
    private static SourcePosition calcLineMappedSourcePosition(PsiFile psiFile, int originalLine) {
        int line = DebuggerUtilsEx.bytecodeToSourceLine(psiFile, originalLine);
        if (line > -1) {
            return SourcePosition.createFromLine(psiFile, line);
        }
        return null;
    }

    public InsidiousJavaDebugProcess getDebugProcess() {
        return this.myDebugProcess;
    }

    @NotNull
    public List<Location> locationsOfLine(@NotNull ReferenceType type, @NotNull SourcePosition position) throws NoDataException {
        try {
            int line = position.getLine() + 1;
            return type.locationsOfLine("Java", null, line);
        } catch (AbsentInformationException absentInformationException) {
            return Collections.emptyList();
        }

    }

    public ClassPrepareRequest createPrepareRequest(@NotNull ClassPrepareRequestor requestor, @NotNull SourcePosition position) throws NoDataException {
        throw new IllegalStateException("This class implements MultiRequestPositionManager, corresponding createPrepareRequests version should be used");
    }

    @NotNull
    public List<ClassPrepareRequest> createPrepareRequests(@NotNull ClassPrepareRequestor requestor, @NotNull SourcePosition position) {
        return ReadAction.compute(() -> {
            List<ClassPrepareRequest> res = new ArrayList<>();
            for (PsiClass psiClass : getLineClasses(position.getFile(), position.getLine())) {
                String classPattern = JVMNameUtil.getNonAnonymousClassName(psiClass);
                if (classPattern == null) {
                    PsiClass parent = JVMNameUtil.getTopLevelParentClass(psiClass);
                    if (parent == null) {
                        continue;
                    }
                    String parentQName = JVMNameUtil.getNonAnonymousClassName(parent);
                    if (parentQName == null) {
                        continue;
                    }
                    classPattern = parentQName + "*";
                }
                ClassPrepareRequest request = this.myDebugProcess.getConnector().createClassPrepareRequest(classPattern, requestor);
                res.add(request);
            }
            return res;
        });
    }

    @Nullable
    public SourcePosition getSourcePosition(Location location) throws NoDataException {
        PositionManagerImpl.ClsSourcePosition clsSourcePosition = null;
        SourcePosition sourcePosition1 = null;
        if (location == null) {
            return null;
        }

        Project project = getDebugProcess().getSession().getProject();
        PsiFile psiFile = getPsiFileByLocation(project, location);
        if (psiFile == null) {
            return null;
        }

        logger.info("Assert debug process is not null - {}", this.myDebugProcess != null);

        int lineNumber = DebuggerUtil.getLineNumber(location, true);

        String qName = location.declaringType().name();


        String altFileUrl = DebuggerUtilsEx.getAlternativeSourceUrl(qName, project);
        if (altFileUrl != null) {
            VirtualFile altFile = VirtualFileManager.getInstance().findFileByUrl(altFileUrl);
            if (altFile != null) {
                PsiFile altPsiFile = psiFile.getManager().findFile(altFile);
                if (altPsiFile != null) {
                    psiFile = altPsiFile;
                }
            }
        }

        SourcePosition sourcePosition = null;
        if (lineNumber > -1) {
            sourcePosition = calcLineMappedSourcePosition(psiFile, lineNumber);
        }

        Method method = DebuggerUtilsEx.getMethod(location);

        if (sourcePosition == null && (psiFile instanceof com.intellij.psi.PsiCompiledElement || lineNumber < 0)) {
            if (method != null && method.name() != null && method.signature() != null) {
                PsiClass psiClass = findPsiClassByName(qName, null);

                PsiMethod compiledMethod = findMethod(
                        (psiClass != null) ? psiClass : psiFile, qName, method

                                .name(), method
                                .signature());
                if (compiledMethod != null) {
                    sourcePosition = SourcePosition.createFromElement(compiledMethod);
                    if (lineNumber >= 0) {
                        clsSourcePosition = new PositionManagerImpl.ClsSourcePosition(sourcePosition, lineNumber);
                    }
                }

            } else {

                return SourcePosition.createFromLine(psiFile, -1);
            }
        }

        if (clsSourcePosition == null) {
            sourcePosition1 = SourcePosition.createFromLine(psiFile, lineNumber);
        }

        int lambdaOrdinal = -1;
        if (DebuggerUtilsEx.isLambda(method)) {
            int line = sourcePosition1.getLine() + 1;


            Set<Method> lambdas = ((StreamEx) StreamEx.of(location.declaringType().methods())
                    .filter(DebuggerUtilsEx::isLambda))
                    .filter(m -> !DebuggerUtil.locationsOfLine((Method) m, line)
                            .isEmpty()).toSet();
            if (lambdas.size() > 1) {
                ArrayList<Method> lambdasList = new ArrayList<>(lambdas);
                lambdasList.sort(DebuggerUtilsEx.LAMBDA_ORDINAL_COMPARATOR);
                lambdaOrdinal = lambdasList.indexOf(method);
            }
        }
        return new PositionManagerImpl.JavaSourcePosition(sourcePosition1, location
                .declaringType(), method, lambdaOrdinal);
    }

    @Nullable
    protected PsiFile getPsiFileByLocation(Project project, Location location) {
        if (location == null) {
            return null;
        }
        ReferenceType refType = location.declaringType();
        if (refType == null) {
            return null;
        }


        String originalQName = refType.name();

        Ref<PsiFile> altSource = new Ref();

        PsiClass psiClass = findPsiClassByName(originalQName, c -> altSource.set(findAlternativeJreSourceFile(c)));


        if (!altSource.isNull()) {
            return altSource.get();
        }

        if (psiClass != null) {
            PsiElement element = psiClass.getNavigationElement();

            if (element instanceof com.intellij.psi.PsiCompiledElement) {
                PsiElement fileElement = psiClass.getContainingFile().getNavigationElement();
                if (!(fileElement instanceof com.intellij.psi.PsiCompiledElement)) {
                    element = fileElement;
                }
            }
            return element.getContainingFile();
        }


        try {
            PsiFile[] files = FilenameIndex.getFilesByName(project, refType
                    .sourceName(), GlobalSearchScope.allScope(project));
            for (PsiFile file : files) {
                if (file instanceof com.intellij.psi.PsiJavaFile) {
                    for (PsiClass cls : PsiTreeUtil.findChildrenOfAnyType(file, PsiClass.class)) {
                        if (StringUtil.equals(originalQName, JVMNameUtil.getClassVMName(cls))) {
                            return file;
                        }
                    }
                }
            }
        } catch (AbsentInformationException absentInformationException) {
        }


        return null;
    }

    private PsiClass findPsiClassByName(String originalQName, @Nullable Consumer<? super ClsClassImpl> altClsProcessor) {
        PsiClass psiClass = null;

        Sdk alternativeJre = this.myDebugProcess.getAlternativeJre();
        if (alternativeJre != null) {
            GlobalSearchScope scope = AlternativeJreClassFinder.getSearchScope(alternativeJre);

            psiClass = findClass(this.myDebugProcess
                    .getSession().getProject(), originalQName, scope, false);
            if (psiClass instanceof ClsClassImpl && altClsProcessor != null) {
                altClsProcessor.accept((ClsClassImpl) psiClass);
            }
        }

        if (psiClass == null) {
            psiClass = findClass(this.myDebugProcess
                    .getSession().getProject(), originalQName, this.myDebugProcess

                    .getSearchScope(), true);
        }

        return psiClass;
    }

    @Nullable
    private PsiFile findAlternativeJreSourceFile(ClsClassImpl psiClass) {
        String sourceFileName = psiClass.getSourceFileName();
        String packageName = ((PsiClassOwner) psiClass.getContainingFile()).getPackageName();


        String relativePath = packageName.isEmpty() ? sourceFileName : (packageName.replace('.', '/') + '/' + sourceFileName);
        Sdk alternativeJre = this.myDebugProcess.getAlternativeJre();

        if (alternativeJre != null) {
            for (VirtualFile file : AlternativeJreClassFinder.getSourceRoots(alternativeJre)) {
                VirtualFile source = file.findFileByRelativePath(relativePath);
                if (source != null && source.isValid()) {
                    PsiFile psiSource = psiClass.getManager().findFile(source);
                    if (psiSource instanceof PsiClassOwner) {
                        return psiSource;
                    }
                }
            }
        }
        return null;
    }

    @NotNull
    public List<ReferenceType> getAllClasses(@NotNull SourcePosition position) {
        return Collections.emptyList();
//        return (List<ReferenceType>) ReadAction.compute(() -> StreamEx.of(getLineClasses(position.getFile(), position.getLine())).flatMap().toList());
    }

    private StreamEx<ReferenceType> getClassReferences(@NotNull PsiClass psiClass, SourcePosition position) {
        boolean isLocalOrAnonymous = false;
        int requiredDepth = 0;

        String className = JVMNameUtil.getNonAnonymousClassName(psiClass);
        if (className == null) {
            isLocalOrAnonymous = true;
            Pair<PsiClass, Integer> enclosing = getTopOrStaticEnclosingClass(psiClass);
            PsiClass topLevelClass = enclosing.first;
            if (topLevelClass != null) {
                String parentClassName = JVMNameUtil.getNonAnonymousClassName(topLevelClass);
                if (parentClassName != null) {
                    requiredDepth = enclosing.second.intValue();
                    className = parentClassName;
                }
            } else {
                StringBuilder sb = new StringBuilder();
                PsiTreeUtil.treeWalkUp(psiClass, null, (element, element2) -> {
                    sb.append('\n').append(element);


                    return true;
                });

                logger.info("Local or anonymous class " + psiClass + " has no non-local parent, parents:" + sb);
            }
        }


        if (className == null) {
            return StreamEx.empty();
        }

        if (!isLocalOrAnonymous) {
            return StreamEx.of(this.myDebugProcess.getConnector().findClassesByName(className));
        }

        int depth = requiredDepth;

        return StreamEx.of(this.myDebugProcess.getConnector().findClassesByName(className))
                .map(outer -> findNested(outer, 0, psiClass, depth, position))
                .nonNull();
    }

    @Nullable
    private ReferenceType findNested(ReferenceType fromClass, int currentDepth, PsiClass classToFind, int requiredDepth, SourcePosition position) {
        ApplicationManager.getApplication().assertReadAccessAllowed();
        if (fromClass.isPrepared()) {
            if (currentDepth < requiredDepth) {
                List<ReferenceType> nestedTypes = fromClass.nestedTypes();
                for (ReferenceType nested : nestedTypes) {

                    ReferenceType found = findNested(nested, currentDepth + 1, classToFind, requiredDepth, position);

                    if (found != null) {
                        return found;
                    }
                }
                return null;
            }

            int rangeBegin = Integer.MAX_VALUE;
            int rangeEnd = Integer.MIN_VALUE;
            List<Location> locations = DebuggerUtilsEx.allLineLocations(fromClass);
            if (locations != null) {
                for (Location location : locations) {
                    int lnumber = DebuggerUtilsEx.getLineNumber(location, false);
                    if (lnumber <= 1) {
                        continue;
                    }


                    Method method = DebuggerUtilsEx.getMethod(location);
                    if (method == null || DebuggerUtils.isSynthetic(method) || method.isBridge()) {
                        continue;
                    }

                    int locationLine = lnumber - 1;
                    PsiFile psiFile = position.getFile().getOriginalFile();
                    if (psiFile instanceof com.intellij.psi.PsiCompiledFile) {
                        locationLine = DebuggerUtilsEx.bytecodeToSourceLine(psiFile, locationLine);
                        if (locationLine < 0)
                            continue;
                    }
                    rangeBegin = Math.min(rangeBegin, locationLine);
                    rangeEnd = Math.max(rangeEnd, locationLine);
                }
            }

            int positionLine = position.getLine();
            if (positionLine >= rangeBegin && positionLine <= rangeEnd) {


                if (!classToFind.isValid()) {
                    return null;
                }
                Set<PsiClass> lineClasses = getLineClasses(position.getFile(), rangeEnd);
                if (lineClasses.size() > 1) {

                    for (PsiClass aClass : lineClasses) {
                        if (classToFind.equals(aClass)) {
                            return fromClass;
                        }
                    }
                } else if (!lineClasses.isEmpty()) {
                    return classToFind.equals(lineClasses.iterator().next()) ? fromClass : null;
                }
                return null;
            }
        }
        return null;
    }

    @Nullable
    public PsiMethod findMethod(PsiElement container, String className, String methodName, String methodSignature) {
        MethodFinder finder = new MethodFinder(className, methodName, methodSignature);
        container.accept(finder);
        return finder.getCompiledMethod();
    }

    private class MethodFinder
            extends JavaRecursiveElementVisitor {
        private final String myClassName;
        private final String myMethodName;
        private final String myMethodSignature;
        private PsiClass myCompiledClass;
        private PsiMethod myCompiledMethod;

        MethodFinder(String className, String methodName, String methodSignature) {
            this.myClassName = className;
            this.myMethodName = methodName;
            this.myMethodSignature = methodSignature;
        }


        public void visitClass(PsiClass aClass) {
            if (this.myCompiledMethod == null) {
                if (InsidiousPositionManager.this.getClassReferences(aClass, SourcePosition.createFromElement(aClass))
                        .anyMatch(referenceType -> referenceType.name().equals(this.myClassName))) {
                    this.myCompiledClass = aClass;
                }

                aClass.acceptChildren(this);
            }
        }


        public void visitMethod(PsiMethod method) {
            if (this.myCompiledMethod == null) {
                String methodName = JVMNameUtil.getJVMMethodName(method);
                PsiClass containingClass = method.getContainingClass();

                if (containingClass != null && containingClass
                        .equals(this.myCompiledClass) && methodName
                        .equals(this.myMethodName) && method
                        .getSignature(PsiSubstitutor.EMPTY)
                        .getName()
                        .equals(this.myMethodSignature)) {
                    this.myCompiledMethod = method;
                }
            }
        }


        public void visitElement(@NotNull PsiElement element) {
            if (this.myCompiledMethod == null) {
                super.visitElement(element);
            }
        }

        @Nullable
        public PsiMethod getCompiledMethod() {
            return this.myCompiledMethod;
        }
    }
}


