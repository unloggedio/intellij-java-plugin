package com.insidious.plugin.client.pojo;

import com.insidious.plugin.extension.model.DataInfo;
import com.insidious.plugin.extension.model.TypeInfo;

import java.util.List;

public class DataResponse<T> {
    private int totalPages;
    private long totalElements;
    private List<T> items;
    private ResponseMetadata metadata;

    public DataResponse(List<T> collect, long totalElements, int totalPages) {
        this.items = collect;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.metadata = new ResponseMetadata();
    }

    public DataResponse(List<T> collect, long totalElements, int totalPages, ResponseMetadata metadata) {
        this.items = collect;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.metadata = metadata;
    }

    public DataResponse() {
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public ResponseMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ResponseMetadata metadata) {
        this.metadata = metadata;
    }

    public DataInfo getDataInfo(String probeId) {
        return metadata.dataInfo.get(probeId);
    }

    public ClassInfo getClassInfo(String probeId) {
        return metadata.classInfo.get(probeId);

    }

    public ObjectInfo getObjectInfo(String probeId) {
        return metadata.objectInfo.get(probeId);

    }

    public TypeInfo getTypeInfo(String probeId) {
        return metadata.typeInfo.get(probeId);

    }
}
