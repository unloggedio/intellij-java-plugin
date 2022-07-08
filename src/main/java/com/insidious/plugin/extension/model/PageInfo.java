package com.insidious.plugin.extension.model;

public class PageInfo {
    Integer number = 0;
    Integer size = 10000;
    Order order = Order.ASC;

    public Integer getNumber() {
        return number;
    }

    public boolean isAsc() {
        return order == Order.ASC;
    }

    public boolean isDesc() {
        return order == Order.DESC;
    }

    public Integer getSize() {
        return size;
    }

    public enum Order {
        ASC, DESC
    }

    public PageInfo() {
    }

    public PageInfo(Integer number, Integer size, Order order) {
        this.number = number;
        this.size = size;
        this.order = order;
    }

    public PageInfo(Integer number) {
        this.number = number;
    }

    public PageInfo(Integer number, Integer size) {
        this.number = number;
        this.size = size;
    }
}
