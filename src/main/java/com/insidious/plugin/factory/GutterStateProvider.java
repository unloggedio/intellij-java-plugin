package com.insidious.plugin.factory;

import com.insidious.plugin.adapter.MethodAdapter;

public interface GutterStateProvider {
    GutterState getGutterStateFor(MethodAdapter method);
}
