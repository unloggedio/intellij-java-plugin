package com.insidious.plugin.pojo;


import com.insidious.common.weaver.EventType;
import com.intellij.openapi.util.text.StringUtil;

import java.util.Collection;
import java.util.List;

public class SearchQuery {
    QueryType queryType;
    Object query;

    public static SearchQuery ByType(Collection<String> classNameList) {
        String query = StringUtil.join(classNameList, ",");
        return new SearchQuery(QueryType.BY_TYPE, query);
    }

    public static SearchQuery ByValue(String query) {
        return new SearchQuery(QueryType.BY_VALUE, query);
    }

    public static SearchQuery ByEvent(Collection<EventType> eventTypeList) {
        return new SearchQuery(QueryType.BY_EVENT, eventTypeList);
    }

    public static SearchQuery ByProbe(Collection<Integer> probeIdList) {
        return new SearchQuery(QueryType.BY_PROBE, probeIdList);
    }

    private SearchQuery(QueryType queryType, Object query) {
        this.queryType = queryType;
        this.query = query;
    }

    public QueryType getQueryType() {
        return queryType;
    }

    public Object getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return queryType + "{" + "query='" + query + '\'' + '}';
    }
}
