package extension.descriptor;

import com.intellij.debugger.engine.evaluation.TextWithImports;
import com.intellij.debugger.impl.descriptors.data.DescriptorData;
import com.intellij.debugger.impl.descriptors.data.DisplayKey;
import com.intellij.debugger.impl.descriptors.data.SimpleDisplayKey;
import com.intellij.openapi.project.Project;
import com.sun.jdi.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InsidiousWatchItemData extends DescriptorData<InsidiousWatchItemDescriptor> {
    private final TextWithImports myText;
    private final Value myValue;

    public InsidiousWatchItemData(TextWithImports text, @Nullable Value value) {
        this.myText = text;
        this.myValue = value;
    }

    protected InsidiousWatchItemDescriptor createDescriptorImpl(@NotNull Project project) {
        return (this.myValue == null) ?
                new InsidiousWatchItemDescriptor(project, this.myText) :
                new InsidiousWatchItemDescriptor(project, this.myText, this.myValue);
    }

    public boolean equals(Object object) {
        if (object instanceof InsidiousWatchItemData) {
            return this.myText.equals(((InsidiousWatchItemData) object).myText);
        }
        return false;
    }

    public int hashCode() {
        return this.myText.hashCode();
    }


    public DisplayKey<InsidiousWatchItemDescriptor> getDisplayKey() {
        return (DisplayKey<InsidiousWatchItemDescriptor>) new SimpleDisplayKey(this.myText.getText());
    }
}

