package com.insidious.plugin.factory;

import com.insidious.plugin.client.VideobugClientInterface;

public class ObjectHistoryConstructor {

    private final VideobugClientInterface client;

    public ObjectHistoryConstructor(VideobugClientInterface client) {
        this.client = client;
    }
}
