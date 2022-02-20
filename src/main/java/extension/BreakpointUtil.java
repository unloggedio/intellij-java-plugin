package extension;

import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.impl.PositionUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiMethod;
import com.intellij.util.PatternUtil;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.sun.jdi.Method;
import extension.evaluation.JVMNameUtil;
import extension.thread.InsidiousVirtualMachineProxy;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class BreakpointUtil {
    public static PsiMethod getPsiMethod(Project project, XBreakpoint breakpoint) {
        SourcePosition sourcePosition = DebuggerUtilsEx.toSourcePosition(breakpoint.getSourcePosition(), project);
        PsiMethod method = PositionUtil.getPsiElementAt(project, PsiMethod.class, sourcePosition);
        return method;
    }

    public static String getMethodName(PsiMethod method) {
        if (method == null) {
            return null;
        }
        return JVMNameUtil.getJVMMethodName(method);
    }

    public static String getSignature(PsiMethod method, InsidiousVirtualMachineProxy proxy) {
        if (method == null) {
            return null;
        }
        try {
            String signature = JVMNameUtil.getJVMSignature(method).getName(proxy);
            return signature;
        } catch (EvaluateException e) {
            e.printStackTrace();

            return null;
        }
    }

    public static boolean matchesWildcardMethod(Method method, String wildcardMethodName) {
        StringBuilder sb = new StringBuilder();
        for (String mask : StringUtil.split(wildcardMethodName, ",")) {
            if (sb.length() > 0) {
                sb.append('|');
            }
            sb.append('(').append(PatternUtil.convertToRegex(mask)).append(')');
        }

        try {
            return (method != null &&
                    Pattern.compile(sb.toString()).matcher(method.name()).matches());
        } catch (PatternSyntaxException e) {
            return false;
        }
    }
}


