package extension;

import com.sun.jdi.ThreadReference;

public class InsidiousConnector {
    public ThreadReference getThreadReferenceWithUniqueId(int uniqueID) {
        return new InsidiousThreadReference();
    }
}
