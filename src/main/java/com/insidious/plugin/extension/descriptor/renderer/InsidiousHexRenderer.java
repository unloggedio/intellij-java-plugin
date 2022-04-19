package com.insidious.plugin.extension.descriptor.renderer;


import com.insidious.plugin.extension.descriptor.InsidiousValueDescriptor;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.openapi.util.text.StringUtil;
import com.sun.jdi.CharValue;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;


public class InsidiousHexRenderer extends InsidiousNodeRendererImpl {
    @NonNls
    public static final String UNIQUE_ID = "HexRenderer";
    private static final Logger logger = LoggerUtil.getInstance(InsidiousHexRenderer.class);

    public InsidiousHexRenderer() {
        super("unnamed", false);
    }

    static void appendHexValue(@NotNull PrimitiveValue value, StringBuilder buf) {
        if (value instanceof CharValue) {
            long longValue = value.longValue();
            buf.append("0x").append(StringUtil.toUpperCase(Long.toHexString(longValue)));
        } else if (value instanceof com.sun.jdi.ByteValue) {
            String strValue = StringUtil.toUpperCase(Integer.toHexString(value.byteValue()));
            if (strValue.length() > 2) {
                strValue = strValue.substring(strValue.length() - 2);
            }
            buf.append("0x").append(strValue);
        } else if (value instanceof com.sun.jdi.ShortValue) {
            String strValue = StringUtil.toUpperCase(Integer.toHexString(value.shortValue()));
            if (strValue.length() > 4) {
                strValue = strValue.substring(strValue.length() - 4);
            }
            buf.append("0x").append(strValue);
        } else if (value instanceof com.sun.jdi.IntegerValue) {
            buf.append("0x").append(StringUtil.toUpperCase(Integer.toHexString(value.intValue())));
        } else if (value instanceof com.sun.jdi.LongValue) {
            buf.append("0x").append(StringUtil.toUpperCase(Long.toHexString(value.longValue())));
        } else {
            logger.info("Assert false");
        }
    }

    public String getUniqueId() {
        return "HexRenderer";
    }

    @NonNls
    public String getName() {
        return "Hex";
    }

    public void setName(String name) {
    }

    public InsidiousHexRenderer clone() {
        return (InsidiousHexRenderer) super.clone();
    }

    public String calcLabel(InsidiousValueDescriptor valueDescriptor, EvaluationContext evaluationContext, DescriptorLabelListener labelListener) {
        Value value = valueDescriptor.getValue();
        StringBuilder buf = new StringBuilder();

        if (value == null)
            return "null";
        if (value instanceof CharValue) {
            InsidiousPrimitiveRenderer.appendCharValue((CharValue) value, buf);
            buf.append(' ');
            appendHexValue((PrimitiveValue) value, buf);
            return buf.toString();
        }
        appendHexValue((PrimitiveValue) value, buf);
        return buf.toString();
    }

    public boolean isApplicable(Type t) {
        if (t == null) {
            return false;
        }
        return (t instanceof com.sun.jdi.CharType || t instanceof com.sun.jdi.ByteType || t instanceof com.sun.jdi.ShortType || t instanceof com.sun.jdi.IntegerType || t instanceof com.sun.jdi.LongType);
    }
}


