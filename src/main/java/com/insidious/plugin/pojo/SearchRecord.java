package com.insidious.plugin.pojo;

import java.util.Date;

public class SearchRecord {
    String query;
    Date lastQueryDate;
    int lastSearchResultCount;

    public SearchRecord(String query, int resultCount) {
        this.query = query;
        this.lastSearchResultCount = resultCount;
        this.lastQueryDate = new Date();
    }

    public SearchRecord() {
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Date getLastQueryDate() {
        return lastQueryDate;
    }

    public void setLastQueryDate(Date lastQueryDate) {
        this.lastQueryDate = lastQueryDate;
    }

    public int getLastSearchResultCount() {
        return lastSearchResultCount;
    }

    public void setLastSearchResultCount(int lastSearchResultCount) {
        this.lastSearchResultCount = lastSearchResultCount;
    }
}
