package extension;

import com.intellij.openapi.util.text.StringUtil;

public class RemoteConnection {
    private boolean myUseSockets;
    private boolean myServerMode;
    private String myHostName;
    private String myAddress;

    public RemoteConnection(boolean useSockets, String hostName, String address, boolean serverMode) {
        this.myUseSockets = useSockets;
        this.myServerMode = serverMode;
        this.myHostName = hostName;
        this.myAddress = address;
    }

    public boolean isUseSockets() {
        return this.myUseSockets;
    }

    public void setUseSockets(boolean useSockets) {
        this.myUseSockets = useSockets;
    }

    public boolean isServerMode() {
        return this.myServerMode;
    }

    public void setServerMode(boolean serverMode) {
        this.myServerMode = serverMode;
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


