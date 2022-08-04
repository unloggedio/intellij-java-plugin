package com.insidious.plugin.client;

import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.client.pojo.ExecutionSession;
import junit.framework.TestCase;
import org.junit.jupiter.api.Test;

import java.io.File;

public class SessionInstanceTest extends TestCase {

    @Test
    public void testTypeIndex() {
        File sessionDirectory = new File("C:\\Users\\artpa\\" +
                ".videobug\\sessions\\selogger-output-9");
        ExecutionSession executionSession = new ExecutionSession();
        SessionInstance sessionInstance = new SessionInstance(sessionDirectory, executionSession);

        TypeInfo type318 = sessionInstance.getTypeInfo(318);
        TypeInfo type317 = sessionInstance.getTypeInfo(317);
        TypeInfo type316 = sessionInstance.getTypeInfo(316);

    }

}