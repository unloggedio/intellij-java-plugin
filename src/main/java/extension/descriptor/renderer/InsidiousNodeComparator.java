package extension.descriptor.renderer;

import extension.InsidiousDebuggerTreeNode;

import java.util.Comparator;

public class InsidiousNodeComparator
        implements Comparator<InsidiousDebuggerTreeNode> {
    public int compare(InsidiousDebuggerTreeNode node1, InsidiousDebuggerTreeNode node2) {
        String name1 = node1.getDescriptor().getName();
        String name2 = node2.getDescriptor().getName();

        boolean invalid1 = (name1 == null || (name1.length() > 0 && Character.isDigit(name1.charAt(0))));

        boolean invalid2 = (name2 == null || (name2.length() > 0 && Character.isDigit(name2.charAt(0))));
        if (invalid1)
            return invalid2 ? 0 : 1;
        if (invalid2) {
            return -1;
        }
        if ("this".equals(name1) || "static".equals(name1)) {
            return -1;
        }
        if ("this".equals(name2) || "static".equals(name2)) {
            return 1;
        }
        return name1.compareToIgnoreCase(name2);
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\render\InsidiousNodeComparator.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */