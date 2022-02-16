package extension.descriptor.renderer;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.JavaHighlightingColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.*;
import com.intellij.ui.icons.RowIcon;
import com.intellij.util.PlatformIcons;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerBundle;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.impl.XDebugSessionImpl;
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil;
import com.intellij.xdebugger.impl.ui.XDebugSessionTab;
import com.intellij.xdebugger.impl.ui.XDebuggerUIConstants;
import com.intellij.xdebugger.impl.ui.tree.ValueMarkup;
import com.sun.jdi.Value;
import extension.descriptor.*;
import extension.evaluation.InsidiousNodeDescriptorImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class InsidiousDebuggerTreeRenderer extends ColoredTreeCellRenderer {
    private static final SimpleTextAttributes DEFAULT_ATTRIBUTES = new SimpleTextAttributes(0, null);

    private static final SimpleTextAttributes SPECIAL_NODE_ATTRIBUTES = new SimpleTextAttributes(0, new JBColor(Color.lightGray, Gray._130));


    private static final SimpleTextAttributes OBJECT_ID_HIGHLIGHT_ATTRIBUTES = new SimpleTextAttributes(0, new JBColor(Color.lightGray, Gray._130));

    @Nullable
    public static Icon getDescriptorIcon(NodeDescriptor descriptor) {
        Icon nodeIcon = null;
        if (descriptor instanceof InsidiousThreadGroupDescriptorImpl) {


            nodeIcon = ((InsidiousThreadGroupDescriptorImpl) descriptor).isCurrent() ? AllIcons.Debugger.ThreadGroupCurrent : AllIcons.Debugger.ThreadGroup;
        } else if (descriptor instanceof InsidiousThreadDescriptorImpl) {
            InsidiousThreadDescriptorImpl threadDescriptor = (InsidiousThreadDescriptorImpl) descriptor;
            nodeIcon = threadDescriptor.getIcon();
        } else if (descriptor instanceof InsidiousStackFrameDescriptorImpl) {
            InsidiousStackFrameDescriptorImpl stackDescriptor = (InsidiousStackFrameDescriptorImpl) descriptor;

            nodeIcon = stackDescriptor.getIcon();
        } else if (descriptor instanceof InsidiousValueDescriptorImpl) {
            nodeIcon = getValueIcon((InsidiousValueDescriptorImpl) descriptor, null);
        } else if (descriptor instanceof InsidiousMessageDescriptor) {
            InsidiousMessageDescriptor messageDescriptor = (InsidiousMessageDescriptor) descriptor;
            if (messageDescriptor.getKind() == 0) {
                nodeIcon = XDebuggerUIConstants.ERROR_MESSAGE_ICON;
            } else if (messageDescriptor.getKind() == 2) {
                nodeIcon = XDebuggerUIConstants.INFORMATION_MESSAGE_ICON;
            } else if (messageDescriptor.getKind() == 3) {
                nodeIcon = null;
            }
        } else if (descriptor instanceof InsidiousStaticDescriptorImpl) {
            nodeIcon = AllIcons.Nodes.Static;
        }

        return nodeIcon;
    }

    public static Icon getValueIcon(InsidiousValueDescriptorImpl valueDescriptor, @Nullable InsidiousValueDescriptorImpl parentDescriptor) {
        Icon icon1 = null;
        RowIcon rowIcon = null;
        if (valueDescriptor instanceof InsidiousFieldDescriptorImpl) {
            LayeredIcon layeredIcon = null;
            InsidiousFieldDescriptorImpl fieldDescriptor = (InsidiousFieldDescriptorImpl) valueDescriptor;
            Icon nodeIcon = PlatformIcons.FIELD_ICON;
            if (parentDescriptor != null) {
                Value value = valueDescriptor.getValue();
                if (value instanceof com.sun.jdi.ObjectReference && value.equals(parentDescriptor.getValue())) {
                    nodeIcon = AllIcons.Debugger.Selfreference;
                }
            }
            if (fieldDescriptor.getField().isFinal()) {
                layeredIcon = new LayeredIcon(nodeIcon, AllIcons.Nodes.FinalMark);
            }
            if (fieldDescriptor.isStatic()) {
                layeredIcon = new LayeredIcon(layeredIcon, AllIcons.Nodes.StaticMark);
            }
        } else if (valueDescriptor instanceof InsidiousThrownExceptionValueDescriptorImpl) {
            icon1 = AllIcons.Nodes.ExceptionClass;
        } else if (valueDescriptor instanceof InsidiousMethodReturnValueDescriptorImpl) {
            icon1 = AllIcons.Debugger.WatchLastReturnValue;
        } else if (isParameter(valueDescriptor)) {
            icon1 = PlatformIcons.PARAMETER_ICON;
        } else if (valueDescriptor.isEnumConstant()) {
            icon1 = PlatformIcons.ENUM_ICON;
        } else if (valueDescriptor.isArray()) {
            icon1 = AllIcons.Debugger.Db_array;
        } else if (valueDescriptor.isPrimitive()) {
            icon1 = AllIcons.Debugger.Db_primitive;
        } else if (valueDescriptor instanceof InsidiousWatchItemDescriptor) {
            icon1 = AllIcons.Debugger.Db_watch;
        } else {
            icon1 = AllIcons.Debugger.Value;
        }

        if (valueDescriptor instanceof InsidiousUserExpressionDescriptorImpl) {

            InsidiousEnumerationChildrenRenderer enumerationChildrenRenderer = InsidiousEnumerationChildrenRenderer.getCurrent(((InsidiousUserExpressionDescriptorImpl) valueDescriptor)

                    .getParentDescriptor());
            if (enumerationChildrenRenderer != null && enumerationChildrenRenderer
                    .isAppendDefaultChildren()) {
                icon1 = AllIcons.Debugger.Db_watch;
            }
        }


        if (valueDescriptor instanceof InsidiousWatchItemDescriptor && icon1 != AllIcons.Debugger.Db_watch) {


            XDebugSession session = XDebuggerManager.getInstance(valueDescriptor.getProject()).getCurrentSession();
            if (session != null) {
                XDebugSessionTab tab = ((XDebugSessionImpl) session).getSessionTab();
                if (tab != null && tab.isWatchesInVariables()) {
                    icon1 = AllIcons.Debugger.Db_watch;
                }
            }
        }

        Icon valueIcon = valueDescriptor.getValueIcon();
        if (icon1 != null && valueIcon != null) {
            rowIcon = IconManager.getInstance().createRowIcon(icon1, valueIcon);
        }
        return rowIcon;
    }

    private static boolean isParameter(InsidiousValueDescriptorImpl valueDescriptor) {
        if (valueDescriptor instanceof InsidiousLocalVariableDescriptorImpl)
            return ((InsidiousLocalVariableDescriptorImpl) valueDescriptor).isParameter();
        if (valueDescriptor instanceof InsidiousArgumentValueDescriptorImpl) {
            return ((InsidiousArgumentValueDescriptorImpl) valueDescriptor).isParameter();
        }
        return false;
    }

    public static SimpleColoredText getDescriptorText(XDebugProcess process, InsidiousNodeDescriptorImpl descriptor, EditorColorsScheme colorsScheme, boolean multiline) {
        return getDescriptorText(process, descriptor, colorsScheme, multiline, true);
    }

    public static SimpleColoredText getDescriptorText(XDebugProcess process, InsidiousNodeDescriptorImpl descriptor, boolean multiline) {
        return getDescriptorText(process, descriptor,
                DebuggerUIUtil.getColorScheme(null), multiline, true);
    }

    public static SimpleColoredText getDescriptorTitle(XDebugProcess process, InsidiousNodeDescriptorImpl descriptor) {
        return getDescriptorText(process, descriptor,
                DebuggerUIUtil.getColorScheme(null), false, false);
    }

    private static SimpleColoredText getDescriptorText(XDebugProcess process, InsidiousNodeDescriptorImpl descriptor, EditorColorsScheme colorScheme, boolean multiline, boolean appendValue) {
        String text, nodeName;
        SimpleColoredText descriptorText = new SimpleColoredText();


        if (descriptor == null) {
            text = "";
            nodeName = null;
        } else {
            text = descriptor.getLabel();
            nodeName = descriptor.getName();
        }

        if (text.equals(XDebuggerBundle.message("xdebugger.building.tree.node.message"))) {
            descriptorText.append(
                    XDebuggerBundle.message("xdebugger.building.tree.node.message"), XDebuggerUIConstants.COLLECTING_DATA_HIGHLIGHT_ATTRIBUTES);

            return descriptorText;
        }

        if (descriptor instanceof InsidiousValueDescriptor) {
            ValueMarkup markup = ((InsidiousValueDescriptor) descriptor).getMarkup(process);
            if (markup != null) {
                descriptorText.append("[" + markup
                        .getText() + "] ", new SimpleTextAttributes(1, markup

                        .getColor()));
            }
        }

        String[] strings = breakString(text, nodeName);

        if (strings[0] != null) {
            if (descriptor instanceof InsidiousMessageDescriptor && ((InsidiousMessageDescriptor) descriptor)
                    .getKind() == 3) {

                descriptorText.append(strings[0], SPECIAL_NODE_ATTRIBUTES);
            } else {
                descriptorText.append(strings[0], DEFAULT_ATTRIBUTES);
            }
        }
        if (strings[1] != null) {
            descriptorText.append(strings[1], XDebuggerUIConstants.VALUE_NAME_ATTRIBUTES);
        }
        if (strings[2] != null) {
            if (descriptor instanceof InsidiousValueDescriptorImpl) {
                if (multiline && strings[2].indexOf('\n') >= 0) {
                    strings = breakString(strings[2], "=");
                    if (strings[2] != null) {
                        strings[2] = strings[0] + strings[1] + "\n" + strings[2];
                    }
                }

                InsidiousValueDescriptorImpl valueDescriptor = (InsidiousValueDescriptorImpl) descriptor;
                String valueLabel = valueDescriptor.getValueLabel();

                strings = breakString(strings[2], valueLabel);
                if (strings[0] != null) {
                    descriptorText.append(strings[0], DEFAULT_ATTRIBUTES);
                }
                if (appendValue && strings[1] != null) {
                    SimpleTextAttributes valueLabelAttribs;
                    if (valueLabel != null &&
                            StringUtil.startsWithChar(valueLabel, '{') && valueLabel
                            .indexOf('}') > 0 &&
                            !StringUtil.endsWithChar(valueLabel, '}')) {
                        int idx = valueLabel.indexOf('}');
                        String objectId = valueLabel.substring(0, idx + 1);
                        valueLabel = valueLabel.substring(idx + 1);
                        descriptorText.append(objectId, OBJECT_ID_HIGHLIGHT_ATTRIBUTES);
                    }

                    valueLabel = DebuggerUtilsEx.truncateString(valueLabel);


                    if (valueDescriptor.isDirty()) {
                        valueLabelAttribs = XDebuggerUIConstants.CHANGED_VALUE_ATTRIBUTES;
                    } else {
                        TextAttributes attributes = null;
                        if (valueDescriptor.isNull()) {
                            attributes = colorScheme.getAttributes(JavaHighlightingColors.KEYWORD);
                        } else if (valueDescriptor.isString()) {
                            attributes = colorScheme.getAttributes(JavaHighlightingColors.STRING);
                        }


                        valueLabelAttribs = (attributes != null) ? SimpleTextAttributes.fromTextAttributes(attributes) : DEFAULT_ATTRIBUTES;
                    }

                    EvaluateException exception = descriptor.getEvaluateException();
                    if (exception != null) {
                        String valueText, errorMessage = exception.getMessage();

                        if (valueLabel.endsWith(errorMessage)) {

                            valueText = valueLabel.substring(0, valueLabel
                                    .length() - errorMessage.length());
                        } else {
                            valueText = valueLabel;
                        }
                        appendValueTextWithEscapesRendering(descriptorText, valueText, valueLabelAttribs, colorScheme);

                        descriptorText.append(errorMessage, XDebuggerUIConstants.EXCEPTION_ATTRIBUTES);

                    } else if (valueLabel.equals(
                            XDebuggerBundle.message("xdebugger.building.tree.node.message"))) {
                        descriptorText.append(
                                XDebuggerBundle.message("xdebugger.building.tree.node.message"), XDebuggerUIConstants.COLLECTING_DATA_HIGHLIGHT_ATTRIBUTES);
                    } else {

                        appendValueTextWithEscapesRendering(descriptorText, valueLabel, valueLabelAttribs, colorScheme);
                    }

                }
            } else {

                descriptorText.append(strings[2], DEFAULT_ATTRIBUTES);
            }
        }

        return descriptorText;
    }

    private static void appendValueTextWithEscapesRendering(SimpleColoredText descriptorText, String valueText, SimpleTextAttributes attribs, EditorColorsScheme colorScheme) {
        SimpleTextAttributes escapeAttribs = null;
        StringBuilder buf = new StringBuilder();
        boolean slashFound = false;
        for (int idx = 0; idx < valueText.length(); idx++) {
            char ch = valueText.charAt(idx);
            if (slashFound) {
                slashFound = false;
                if (ch == '\\' || ch == '"' || ch == 'b' || ch == 't' || ch == 'n' || ch == 'f' || ch == 'r') {


                    if (buf.length() > 0) {
                        descriptorText.append(buf.toString(), attribs);
                        buf.setLength(0);
                    }

                    if (escapeAttribs == null) {

                        TextAttributes fromHighlighter = colorScheme.getAttributes(JavaHighlightingColors.VALID_STRING_ESCAPE);

                        if (fromHighlighter != null) {

                            escapeAttribs = SimpleTextAttributes.fromTextAttributes(fromHighlighter);
                        } else {

                            escapeAttribs = DEFAULT_ATTRIBUTES.derive(1, JBColor.GRAY, null, null);
                        }
                    }


                    if (ch != '\\' && ch != '"') {
                        descriptorText.append("\\", escapeAttribs);
                    }
                    descriptorText.append(String.valueOf(ch), escapeAttribs);
                } else {
                    buf.append('\\').append(ch);
                }

            } else if (ch == '\\') {
                slashFound = true;
            } else {
                buf.append(ch);
            }
        }

        if (buf.length() > 0) {
            descriptorText.append(buf.toString(), attribs);
        }
    }

    private static String[] breakString(String source, String substr) {
        if (substr != null && substr.length() > 0) {
            int index = Math.max(source.indexOf(substr), 0);
            String prefix = (index > 0) ? source.substring(0, index) : null;
            index += substr.length();
            String suffix = (index < source.length() - 1) ? source.substring(index) : null;
            return new String[]{prefix, substr, suffix};
        }
        return new String[]{source, null, null};
    }

    public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        InsidiousDebuggerTreeNodeImpl node = (InsidiousDebuggerTreeNodeImpl) value;

        if (node != null) {
            SimpleColoredText text = node.getText();
            if (text != null) {
                text.appendToComponent(this);
            }
            setIcon(node.getIcon());
        }
    }
}


