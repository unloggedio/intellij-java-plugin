package com.insidious.plugin.extension;

import com.insidious.plugin.client.pojo.exceptions.UnauthorizedException;
import com.insidious.plugin.util.LoggerUtil;
import org.slf4j.Logger;

public class RemoteConnection {
    private final static Logger logger = LoggerUtil.getInstance(RemoteConnection.class);
    private final String address;
    private boolean myUseSockets;
    private String myHostName;

    public RemoteConnection(String address) throws UnauthorizedException {
        this.address = address;
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
        String address = this.address;
        return "'" + address + "', transport: '" + transportName + "'";
    }
}


