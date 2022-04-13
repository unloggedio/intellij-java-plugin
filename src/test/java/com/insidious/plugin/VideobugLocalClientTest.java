package com.insidious.plugin;

import com.insidious.plugin.network.VideobugClientInterface;
import com.insidious.plugin.network.VideobugLocalClient;
import org.junit.Test;

public class VideobugLocalClientTest {


    @Test
    public void testLocalClient1() {
        VideobugClientInterface client = new VideobugLocalClient("src/main/resources/test-output-1649835320389");

        client.getTracesByObjectValue();
    }

}