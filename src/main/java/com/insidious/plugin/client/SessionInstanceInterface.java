package com.insidious.plugin.client;

import java.util.List;

import com.insidious.common.weaver.TypeInfo;

public interface SessionInstanceInterface {
    public boolean isScanEnable();
	public TypeInfo getTypeInfo(String name);
	public TypeInfo getTypeInfo(Integer typeId);
	public int getTotalFileCount();
    public List<UnloggedTimingTag> getTimingTags(long id);
}
