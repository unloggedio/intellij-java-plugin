package callbacks;

import network.pojo.ExceptionResponse;
import pojo.DataEvent;

import java.util.List;

public interface FilteredDataEventsCallback {
    void error(ExceptionResponse errorResponse);

    void success(List<DataEvent> dataList);
}
