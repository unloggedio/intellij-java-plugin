package extension.smartstep;

import com.intellij.debugger.NoDataException;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiStatement;
import com.intellij.util.Range;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import extension.InsidiousVirtualMachineProxy;
import org.jetbrains.annotations.Nullable;

public class LambdaMethodFilter
        implements BreakpointStepMethodFilter {
    private final PsiLambdaExpression myLambda;
    private final int myLambdaOrdinal;
    @Nullable
    private final SourcePosition myFirstStatementPosition;
    private final int myLastStatementLine;
    private final Range<Integer> myCallingExpressionLines;

    public LambdaMethodFilter(PsiLambdaExpression lambda, int expressionOrdinal, Range<Integer> callingExpressionLines) {
        this.myLambda = lambda;
        this.myLambdaOrdinal = expressionOrdinal;
        this.myCallingExpressionLines = callingExpressionLines;

        SourcePosition firstStatementPosition = null;
        SourcePosition lastStatementPosition = null;
        PsiElement body = lambda.getBody();
        if (body instanceof PsiCodeBlock) {
            PsiStatement[] statements = ((PsiCodeBlock) body).getStatements();
            if (statements.length > 0) {
                firstStatementPosition = SourcePosition.createFromElement(statements[0]);
                if (firstStatementPosition != null) {
                    PsiStatement lastStatement = statements[statements.length - 1];

                    lastStatementPosition = SourcePosition.createFromOffset(firstStatementPosition
                            .getFile(), lastStatement
                            .getTextRange().getEndOffset());
                }
            }
        } else if (body != null) {
            firstStatementPosition = SourcePosition.createFromElement(body);
        }
        this.myFirstStatementPosition = firstStatementPosition;
        this.myLastStatementLine = (lastStatementPosition != null) ? lastStatementPosition.getLine() : -1;
    }

    public int getLambdaOrdinal() {
        return this.myLambdaOrdinal;
    }


    @Nullable
    public SourcePosition getBreakpointPosition() {
        return this.myFirstStatementPosition;
    }


    public int getLastStatementLine() {
        return this.myLastStatementLine;
    }


    public boolean locationMatches(InsidiousVirtualMachineProxy virtualMachineProxy, Location location) {
        Method method = location.method();
        if (DebuggerUtilsEx.isLambda(method) && (
                !virtualMachineProxy.canGetSyntheticAttribute() || method.isSynthetic())) {
            SourcePosition position = null;
            try {
                position = virtualMachineProxy.getPositionManager().getSourcePosition(location);
            } catch (NoDataException noDataException) {
            }

            if (position != null) {
                SourcePosition finalPosition = position;
                return ReadAction.compute(() -> Boolean.valueOf(DebuggerUtilsEx.inTheMethod(finalPosition, this.myLambda))).booleanValue();
            }
        }

        return false;
    }


    @Nullable
    public Range<Integer> getCallingExpressionLines() {
        return this.myCallingExpressionLines;
    }
}


