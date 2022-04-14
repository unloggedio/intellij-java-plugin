package com.insidious.plugin.callbacks;

import com.insidious.plugin.videobugclient.pojo.ExceptionResponse;
import com.insidious.plugin.pojo.DataEvent;

import java.util.List;

public interface FilteredDataEventsCallback {
    void error(ExceptionResponse errorResponse);

    void success(List<DataEvent> dataList);
}
