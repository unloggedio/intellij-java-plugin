package com.insidious.plugin.ui.methodscope;

import java.util.List;
import java.util.Map;

public class DifferenceResult {
    private final List<DifferenceInstance> differenceInstanceList;
    private final DiffResultType diffResultType;
    private final Map<String, Object> leftOnly;
    private final Map<String, Object> rightOnly;

    public Map<String, Object> getLeftOnly() {
        return leftOnly;
    }

    public Map<String, Object> getRightOnly() {
        return rightOnly;
    }

    public DifferenceResult(List<DifferenceInstance> differenceInstanceList,
                            DiffResultType diffResultType,
                            Map<String, Object> leftOnly,
                            Map<String, Object> rightOnly) {
        this.differenceInstanceList = differenceInstanceList;
        this.diffResultType = diffResultType;
        this.leftOnly = leftOnly;
        this.rightOnly = rightOnly;
    }

    public List<DifferenceInstance> getDifferenceInstanceList() {
        return differenceInstanceList;
    }

    public DiffResultType getDiffResultType() {
        return diffResultType;
    }
}
