package com.insidious.plugin.extension;

import com.insidious.plugin.videobugclient.VideobugClientInterface;
import com.insidious.plugin.util.LoggerUtil;
import org.slf4j.Logger;
import com.insidious.plugin.videobugclient.pojo.exceptions.UnauthorizedException;

public class RemoteConnection {
    private final static Logger logger = LoggerUtil.getInstance(RemoteConnection.class);
    private final VideobugClientInterface client;
    private boolean myUseSockets;
    private String myHostName;

    public RemoteConnection(String address, VideobugClientInterface client) throws UnauthorizedException {
        this.client = client;
    }


    public VideobugClientInterface getClient() {
        return client;
    }

    public boolean isUseSockets() {
        return this.myUseSockets;
    }

    public void setUseSockets(boolean useSockets) {
        this.myUseSockets = useSockets;
    }

    public String getHostName() {
        return this.myHostName;
    }

    public void setHostName(String hostName) {
        this.myHostName = hostName;
    }

    public String toString() {
        String transportName = "InsidiousVM";
        String address = client.getEndpoint();
        return "'" + address + "', transport: '" + transportName + "'";
    }
}


