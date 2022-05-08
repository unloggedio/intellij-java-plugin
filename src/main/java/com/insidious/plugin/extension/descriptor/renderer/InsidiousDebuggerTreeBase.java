package com.insidious.plugin.extension.descriptor.renderer;

import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.impl.TipManager;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.text.StringTokenizer;
import com.intellij.util.ui.GeometryUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import com.insidious.plugin.extension.descriptor.InsidiousValueDescriptorImpl;
import com.insidious.plugin.extension.evaluation.InsidiousNodeDescriptorImpl;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class InsidiousDebuggerTreeBase extends DnDAwareTree implements Disposable {
    protected final TipManager myTipManager;
    private final Project myProject;
    private final InsidiousDebuggerTreeRenderer cellRenderer;
    private InsidiousDebuggerTreeNodeImpl myCurrentTooltipNode;
    private JComponent myCurrentTooltip;

    public InsidiousDebuggerTreeBase(TreeModel model, Project project) {
        super(model);
        this.myProject = project;

        this.myTipManager = new TipManager(this, new TipManager.TipFactory() {
            public JComponent createToolTip(MouseEvent e) {
                return InsidiousDebuggerTreeBase.this.createToolTip(e);
            }

            public MouseEvent createTooltipEvent(MouseEvent candidateEvent) {
                return InsidiousDebuggerTreeBase.this.createTooltipEvent(candidateEvent);
            }


            public boolean isFocusOwner() {
                return InsidiousDebuggerTreeBase.this.isFocusOwner();
            }
        });

        Disposer.register(this, this.myTipManager);

        setRootVisible(false);
        setShowsRootHandles(true);
        cellRenderer = new InsidiousDebuggerTreeRenderer();
        setCellRenderer(cellRenderer);
        updateUI();
        TreeUtil.installActions(this);
    }

    private JComponent createTipContent(String tipText, InsidiousDebuggerTreeNodeImpl node) {
        JToolTip tooltip = new JToolTip();

        if (tipText == null) {
            tooltip.setTipText(tipText);
        } else {
            Dimension rootSize = getVisibleRect().getSize();
            Insets borderInsets = tooltip.getBorder().getBorderInsets(tooltip);
            rootSize.width -= (borderInsets.left + borderInsets.right) * 2;
            rootSize.height -= (borderInsets.top + borderInsets.bottom) * 2;

            StringBuilder tipBuilder = new StringBuilder();
            String markupText = node.getMarkupTooltipText();
            if (markupText != null) {
                tipBuilder.append(markupText);
            }

            if (!tipText.isEmpty()) {
                StringTokenizer tokenizer = new StringTokenizer(tipText, "\n ", true);

                while (tokenizer.hasMoreElements()) {
                    String each = tokenizer.nextElement();
                    if ("\n".equals(each)) {
                        tipBuilder.append("<br>");
                        continue;
                    }
                    if (" ".equals(each)) {
                        tipBuilder.append("&nbsp ");
                        continue;
                    }
                    tipBuilder.append(JDOMUtil.legalizeText(each));
                }
            }


            tooltip.setTipText(UIUtil.toHtml(tipBuilder.toString(), 0));
        }

        tooltip.setBorder(null);

        return tooltip;
    }

    public MouseEvent createTooltipEvent(MouseEvent candidate) {
        TreePath path = null;

        if (candidate != null) {

            Point treePoint = SwingUtilities.convertPoint(candidate
                    .getComponent(), candidate.getPoint(), this);
            if (GeometryUtil.isWithin(new Rectangle(0, 0, getWidth(), getHeight()), treePoint)) {
                path = getPathForLocation(treePoint.x, treePoint.y);
            }
        }

        if (path == null &&
                isFocusOwner()) {
            path = getSelectionPath();
        }


        if (path == null) return null;

        int row = getRowForPath(path);
        if (row == -1) return null;

        Rectangle bounds = getRowBounds(row);

        return new MouseEvent(this, 503,


                System.currentTimeMillis(), 0, bounds.x, bounds.y + bounds.height - bounds.height / 4, 0, false);
    }


    @Nullable
    public JComponent createToolTip(MouseEvent e) {
        InsidiousDebuggerTreeNodeImpl node = getNodeToShowTip(e);
        if (node == null) {
            return null;
        }

        if (this.myCurrentTooltip != null && this.myCurrentTooltip
                .isShowing() && this.myCurrentTooltipNode == node) {
            return this.myCurrentTooltip;
        }

        String toolTipText = getTipText(node);
        if (toolTipText == null) {
            return null;
        }

        JComponent tipContent = createTipContent(toolTipText, node);
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(tipContent);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(30);
        scrollPane.setVerticalScrollBarPolicy(20);

        Point point = e.getPoint();
        SwingUtilities.convertPointToScreen(point, e.getComponent());
        Rectangle tipRectangle = new Rectangle(point, tipContent.getPreferredSize());

        Rectangle screen = ScreenUtil.getScreenRectangle(point.x, point.y);

        JToolTip toolTip = new JToolTip();

        tipContent.addMouseListener(new HideTooltip(toolTip));

        Border tooltipBorder = toolTip.getBorder();
        if (tooltipBorder != null) {
            Insets borderInsets = tooltipBorder.getBorderInsets(this);
            tipRectangle.setSize(tipRectangle.width + borderInsets.left + borderInsets.right, tipRectangle.height + borderInsets.top + borderInsets.bottom);
        }


        toolTip.setLayout(new BorderLayout());
        toolTip.add(scrollPane, "Center");

        tipRectangle.height += (scrollPane.getHorizontalScrollBar().getPreferredSize()).height;
        tipRectangle.width += (scrollPane.getVerticalScrollBar().getPreferredSize()).width;

        int maxWidth = (int) (screen.width - screen.width * 0.25D);
        if (tipRectangle.width > maxWidth) {
            tipRectangle.width = maxWidth;
        }

        Dimension prefSize = tipRectangle.getSize();

        ScreenUtil.cropRectangleToFitTheScreen(tipRectangle);

        if (prefSize.width > tipRectangle.width) {
            int delta = prefSize.width - tipRectangle.width;
            tipRectangle.x -= delta;
            if (tipRectangle.x < screen.x) {
                screen.x += maxWidth / 2;
                screen.width -= maxWidth / 2;
            } else {
                tipRectangle.width += delta;
            }
        }

        toolTip.setPreferredSize(tipRectangle.getSize());

        this.myCurrentTooltip = toolTip;
        this.myCurrentTooltipNode = node;

        return this.myCurrentTooltip;
    }

    @Nullable
    private String getTipText(InsidiousDebuggerTreeNodeImpl node) {
        InsidiousNodeDescriptorImpl descriptor = node.getDescriptor();
        if (descriptor instanceof InsidiousValueDescriptorImpl) {
            String text = ((InsidiousValueDescriptorImpl) descriptor).getValueText();
            String tipText = DebuggerUtilsEx.prepareValueText(text, this.myProject);
            if (!tipText.isEmpty()) if (tipText
                    .indexOf('\n') < 0) {
                if (!getVisibleRect().contains(
                        getRowBounds(
                                getRowForPath(new TreePath(node
                                        .getPath())))))
                    return tipText;
            } else {
                return tipText;
            }

        }
        return (node.getMarkupTooltipText() != null) ? "" : null;
    }

    @Nullable
    private InsidiousDebuggerTreeNodeImpl getNodeToShowTip(MouseEvent event) {
        TreePath path = getPathForLocation(event.getX(), event.getY());
        if (path != null) {
            Object last = path.getLastPathComponent();
            if (last instanceof InsidiousDebuggerTreeNodeImpl) {
                return (InsidiousDebuggerTreeNodeImpl) last;
            }
        }

        return null;
    }


    public void dispose() {
        JComponent tooltip = this.myCurrentTooltip;
        if (tooltip != null) {
            tooltip.setVisible(false);
        }
        this.myCurrentTooltip = null;
        this.myCurrentTooltipNode = null;
    }

    public Project getProject() {
        return this.myProject;
    }

    private static class HideTooltip extends MouseAdapter {
        private final JToolTip myToolTip;

        HideTooltip(JToolTip toolTip) {
            this.myToolTip = toolTip;
        }


        public void mouseReleased(MouseEvent e) {
            if (UIUtil.isActionClick(e)) {
                Window wnd = SwingUtilities.getWindowAncestor(this.myToolTip);
                if (wnd instanceof javax.swing.JWindow)
                    wnd.setVisible(false);
            }
        }
    }
}

