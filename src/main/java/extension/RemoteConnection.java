package extension;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import network.Client;
import network.pojo.exceptions.UnauthorizedException;

public class RemoteConnection {
    private final static Logger logger = Logger.getInstance(RemoteConnection.class);
    private final Client client;
    private boolean myUseSockets;
    private String myHostName;
    private String myAddress;

    public RemoteConnection(String address, Client client) throws UnauthorizedException {
        this.client = client;
        this.myAddress = address;
    }


    public Client getClient() {
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

    public String getAddress() {
        return this.myAddress;
    }

    public void setAddress(String address) {
        this.myAddress = address;
    }

    public int getPort() {
        try {
            return Integer.parseInt(this.myAddress);
        } catch (NumberFormatException e) {
            throw new Error("should never happen: " + e);
        }
    }

    public String toString() {
        String transportName = isUseSockets() ? "socket" : "shared memory";


        String address = isUseSockets() ? (StringUtil.notNullize(getHostName()) + ":" + getAddress()) : getAddress();
        return "'" + address + "', transport: '" + transportName + "'";
    }
}


