package extension;

import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import network.Client;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class RemoteConnection {
    private final Client client;
    private final String projectName;
    private boolean myUseSockets;
    private String myHostName;
    private String myAddress;

    public RemoteConnection(String address, @NotNull @NlsSafe String projectName) {
        Client client = null;
        try {
            client = new Client(address, projectName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.myAddress = address;
        this.client = client;
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
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


