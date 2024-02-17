package com.insidious.plugin.ui.library;

public interface ItemLifeCycleListener<T> {
    void onSelect(T item);
    void onClick(T item);
    void onUnSelect(T item);
    void onDelete(T item);
    void onEdit(T item);
}
