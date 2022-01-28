package callbacks;

import network.pojo.ExceptionResponse;

public interface FilteredDataEventsCallback {
    void error(ExceptionResponse errrorResponse);

    void success();
}
