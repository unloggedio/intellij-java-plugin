package com.insidious.plugin.ui;

import com.insidious.common.weaver.DataInfo;

import javax.swing.tree.DefaultMutableTreeNode;

public class ProbeInfoModel extends DefaultMutableTreeNode {

    private final DataInfo dataInfo;


    public DataInfo getDataInfo() {
        return dataInfo;
    }

    public int getProbeId() {
        return dataInfo.getDataId();
    }

    public ProbeInfoModel(DataInfo dataInfo) {
        this.dataInfo = dataInfo;
    }

    @Override
    public String toString() {
        return "#" + dataInfo.getLine() + " " + "[" + dataInfo.getDataId() + "]" + "[" + dataInfo.getEventType().toString() + "] "
                + "value = " + dataInfo.getValueDesc().getString() + " [" + dataInfo.getAttributes() + "]";
    }
}
