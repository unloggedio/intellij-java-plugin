package com.insidious.plugin.client;

import com.insidious.plugin.pojo.dao.Parameter;

public interface ParameterProvider {
    Parameter getParameterByValue(Long value);
}
