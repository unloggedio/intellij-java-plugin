package extension.connector;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;


public class DecompiledLocalVariable {
    public static final String PARAM_PREFIX = "param_";
    public static final String SLOT_PREFIX = "slot_";
    private final int mySlot;
    private final String mySignature;
    private final boolean myIsParam;
    private final Collection<String> myMatchedNames;

    public DecompiledLocalVariable(int slot, boolean isParam, @Nullable String signature, @NotNull Collection<String> names) {
        this.mySlot = slot;
        this.myIsParam = isParam;
        this.mySignature = signature;
        this.myMatchedNames = names;
    }

    public static int getParamId(@Nullable String name) {
        if (!StringUtil.isEmpty(name)) {
            return StringUtil.parseInt(StringUtil.substringAfter(name, "param_"), -1);
        }
        return -1;
    }

    public int getSlot() {
        return this.mySlot;
    }

    @Nullable
    public String getSignature() {
        return this.mySignature;
    }

    public boolean isParam() {
        return this.myIsParam;
    }

    @NotNull
    public String getDefaultName() {
        return (this.myIsParam ? "param_" : "slot_") + this.mySlot;
    }

    public String getDisplayName() {
        String nameString = StringUtil.join(this.myMatchedNames, " | ");
        if (this.myIsParam && this.myMatchedNames.size() == 1)
            return nameString;
        if (!this.myMatchedNames.isEmpty()) {
            return nameString + " (" + getDefaultName() + ")";
        }
        return getDefaultName();
    }

    @NotNull
    public Collection<String> getMatchedNames() {
        return this.myMatchedNames;
    }

    public String toString() {
        return getDisplayName() + " (slot " + this.mySlot + ", " + this.mySignature + ")";
    }
}

