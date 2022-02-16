package extension.descriptor;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.util.containers.ContainerUtil;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public interface InsidiousReferringObjectsProvider {
    InsidiousReferringObjectsProvider BASIC_JDI = new InsidiousReferringObjectsProvider() {


        @NotNull
        public List<InsidiousReferringObject> getReferringObjects(@NotNull EvaluationContext evaluationContext, @NotNull ObjectReference value, long limit) {
            return ContainerUtil.map(value.referringObjects(limit), x -> asReferringObject(x, value));
        }


        private InsidiousReferringObject asReferringObject(@NotNull ObjectReference referrer, @NotNull ObjectReference referee) {
            Field field = findField(referee, referrer);
            if (field != null) {
                return new InsidiousFieldReferringObject(referrer, field);
            }

            return new InsidiousSimpleReferringObject(referrer);
        }

        @Nullable
        private Field findField(@NotNull Value value, @NotNull ObjectReference reference) {
            for (Field field : reference.referenceType().allFields()) {
                if (reference.getValue(field) == value) {
                    return field;
                }
            }

            return null;
        }
    };

    @NotNull
    List<InsidiousReferringObject> getReferringObjects(@NotNull EvaluationContext paramEvaluationContext, @NotNull ObjectReference paramObjectReference, long paramLong) throws EvaluateException;
}

