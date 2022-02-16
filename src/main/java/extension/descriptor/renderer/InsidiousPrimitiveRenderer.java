package extension.descriptor.renderer;


import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.sun.jdi.CharValue;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import extension.DebuggerBundle;
import extension.descriptor.InsidiousValueDescriptor;
import extension.evaluation.EvaluationContext;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;


public class InsidiousPrimitiveRenderer extends InsidiousNodeRendererImpl {
    @NonNls
    public static final String UNIQUE_ID = "PrimitiveRenderer";
    public boolean SHOW_HEX_VALUE = false;

    public InsidiousPrimitiveRenderer() {
        super("Primitive");
    }

    static void appendCharValue(CharValue value, StringBuilder buf) {
        buf.append('\'');
        String s = value.toString();
        StringUtil.escapeStringCharacters(s.length(), s, "'", buf);
        buf.append('\'');
    }

    private static void appendHexValue(PrimitiveValue value, StringBuilder buf) {
        if (RendererManager.getInstance().getHexRenderer().isApplicable(value.type())) {
            buf.append(" (");
            InsidiousHexRenderer.appendHexValue(value, buf);
            buf.append(')');
        }
    }

    public String getUniqueId() {
        return "PrimitiveRenderer";
    }

    public void setName(String text) {
    }

    public final boolean isEnabled() {
        return true;
    }

    public void setEnabled(boolean enabled) {
    }

    public boolean isApplicable(Type type) {
        return (type == null || type instanceof com.sun.jdi.PrimitiveType || type instanceof com.sun.jdi.VoidType);
    }

    public String calcLabel(InsidiousValueDescriptor valueDescriptor, EvaluationContext evaluationContext, DescriptorLabelListener labelListener) {
        Value value = valueDescriptor.getValue();
        if (value == null)
            return "null";
        if (value instanceof PrimitiveValue) {
            if (value instanceof CharValue) {
                StringBuilder buf = new StringBuilder();
                appendCharValue((CharValue) value, buf);
                if (this.SHOW_HEX_VALUE) {
                    appendHexValue((CharValue) value, buf);
                } else {
                    buf.append(' ').append(((PrimitiveValue) value).longValue());
                }
                return buf.toString();
            }
            if (this.SHOW_HEX_VALUE) {
                StringBuilder buf = new StringBuilder();
                buf.append(value);
                appendHexValue((PrimitiveValue) value, buf);
                return buf.toString();
            }
            return value.toString();
        }


        return DebuggerBundle.message("label.undefined");
    }

    public boolean isShowHexValue() {
        return this.SHOW_HEX_VALUE;
    }

    public void setShowHexValue(boolean show) {
        this.SHOW_HEX_VALUE = show;
    }


    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        DefaultJDOMExternalizer.readExternal(this, element);
    }


    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);

        if (this.SHOW_HEX_VALUE)
            JDOMExternalizerUtil.writeField(element, "SHOW_HEX_VALUE", "true");
    }
}


