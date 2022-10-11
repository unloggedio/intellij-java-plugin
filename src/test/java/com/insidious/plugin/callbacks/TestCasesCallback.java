package com.insidious.plugin.callbacks;

import java.util.List;

public interface TestCasesCallback {
    void error(String message);

    void success(List<String> testcases);
}
