package network.pojo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataResponse<T> {
    private int totalPages;
    private long totalElements;
    private List<T> items;
    private Map<String, Object> metadata;

    public DataResponse(List<T> collect, long totalElements, int totalPages) {
        this.items = collect;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.metadata = new HashMap<>();
    }

    public DataResponse(List<T> collect, long totalElements, int totalPages, Map<String, Object> metadata) {
        this.items = collect;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.metadata = metadata;
    }

    public DataResponse() {
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
