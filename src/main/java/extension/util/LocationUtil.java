package extension.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import extension.CommandSender;
import extension.connector.InsidiousJDIConnector;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class LocationUtil {
    private static final Logger logger = Logger.getInstance(LocationUtil.class);
    private static final boolean isHeadlessMode = Boolean.getBoolean("is.headless.test");

    public static String getFormattedClassName(String className) {
        String formattedClassName = className.replace("/", ".");
        int endIndex = formattedClassName.indexOf("$");
        if (endIndex > -1) {
            formattedClassName = formattedClassName.substring(0, endIndex);
        }
        return formattedClassName;
    }

    public static PsiClass findPsiClass(Project project, String className) {
        String finalClassName = getFormattedClassName(className);
        AtomicReference<PsiClass> psiClass = new AtomicReference<>();
        ApplicationManager.getApplication()
                .invokeAndWait(() -> psiClass.set(JavaPsiFacade.getInstance(project).findClass(finalClassName, GlobalSearchScope.allScope(project))));


        PsiClass result = psiClass.get();
        if (result != null &&
                className.contains("$")) {
            String seachedInnerClass = className.substring(className.lastIndexOf('$') + 1);
            for (PsiClass innerClass : result.getAllInnerClasses()) {
                if (innerClass.getName().equals(seachedInnerClass)) {
                    result = innerClass;

                    break;
                }
            }
        }
        return result;
    }

    public static PsiFile findPsiFile(Project project, String className) {
        return findPsiFile(findPsiClass(project, className));
    }

    public static PsiFile findPsiFile(PsiClass psiClass) {
        PsiElement psiClassSource = psiClass.getNavigationElement();
        return psiClassSource.getContainingFile();
    }

    public static XSourcePosition createSourcePosition(Project project, Location location) {
        return null;
//        XSourcePosition position = null;
//        try {
//            PsiFile psiFile = findPsiFile(project, location.getClassName());
//            VirtualFile sourceFile = psiFile.getVirtualFile();
//
//
//            position = XDebuggerUtil.getInstance().createPosition(sourceFile, location.getLineNumber() - 1);
//        } catch (Exception ex) {
//            logger.debug(
//                    String.format("The action will be disabled, couldn't evaluate position for location %s:", location.toString()), ex);
//        }
//        return position;
    }


    public static Optional<Integer> getMethodStartLine(Project project, PsiClass psiClass, int lineNumber) {
        PsiFile psiFile = findPsiFile(psiClass);
        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);

        int lineNumberOffset = document.getLineStartOffset(lineNumber);
        AtomicReference<PsiMethod[]> methods = new AtomicReference<>();
        ApplicationManager.getApplication()
                .invokeAndWait(() -> methods.set(psiClass.getAllMethods()));


        Optional<PsiMethod> foundMethod = Arrays.stream(methods.get()).filter(x -> x.getTextRange().contains(lineNumberOffset)).findAny();

        if (foundMethod.isPresent()) {
            PsiMethod method = foundMethod.get();
            int startOffset = method.getTextRange().getStartOffset();
            int methodStartLineNo = document.getLineNumber(startOffset);
            return Optional.ofNullable(Integer.valueOf(methodStartLineNo));
        }
        return Optional.empty();
    }

    public static Location getCurrentLocation(CommandSender commandSender) {
        try {
            InsidiousJDIConnector connector = commandSender.getDebugProcess().getConnector();

            ThreadReference currentThread = connector.getThreadReferenceWithUniqueId(commandSender.getLastThreadId());
            StackFrame frame = currentThread.frame(0);
            if (frame != null) {
                String className = frame.location().declaringType().name();
                String methodName = frame.location().method().name();
                int lineNumber = frame.location().lineNumber();
                return null;
//                return new Location("current", className, methodName, lineNumber);
            }
            return null;
        } catch (Exception ex) {
            logger.debug("Couldn't determine current location", ex);

            return null;
        }
    }

    public static boolean isLocationStartOfLine(Location location) throws AbsentInformationException {
        return false;
//        int currentLine = location.getLineNumber();
//        List<Location> locations = location.method().locationsOfLine(currentLine);
//
//        Location firstBci = locations.stream().min(Comparable::compareTo).get();
//      return firstBci.codeIndex() == location.codeIndex();
    }
}


