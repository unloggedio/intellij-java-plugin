package callbacks;

import network.pojo.ExceptionResponse;
import pojo.VarsValues;

import java.util.ArrayList;
import java.util.List;

public interface FilteredDataEventsCallback {
    void error(ExceptionResponse errorResponse);

    void success(List<VarsValues> dataList);
}
