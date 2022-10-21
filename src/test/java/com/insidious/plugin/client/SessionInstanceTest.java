package com.insidious.plugin.client;

import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.client.pojo.ExecutionSession;
import junit.framework.TestCase;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class SessionInstanceTest extends TestCase {

    @Test
    public void testTypeIndex() throws SQLException, IOException {
        File sessionDirectory = new File("C:\\Users\\artpa\\" +
                ".videobug\\sessions\\selogger-output-9");
        ExecutionSession executionSession = new ExecutionSession();
        SessionInstance sessionInstance = new SessionInstance(executionSession);

        TypeInfo type318 = sessionInstance.getTypeInfo(318);
        TypeInfo type317 = sessionInstance.getTypeInfo(317);
        TypeInfo type316 = sessionInstance.getTypeInfo(316);

    }

}