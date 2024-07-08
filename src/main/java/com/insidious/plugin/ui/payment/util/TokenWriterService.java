package com.insidious.plugin.ui.payment.util;

public interface TokenWriterService {
    String readToken();
    void writeToken(String token);
    void removeToken();
}
