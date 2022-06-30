package com.insidious.plugin.pojo;


import com.intellij.openapi.util.text.StringUtil;

import java.util.Collection;

public class SearchQuery {
    QueryType queryType;
    String query;

    public static SearchQuery ByType(Collection<String> classNameList) {
        String query = StringUtil.join(classNameList, ",");
        return new SearchQuery(QueryType.BY_TYPE, query);
    }

    public static SearchQuery ByValue(String query) {
        return new SearchQuery(QueryType.BY_VALUE, query);
    }

    private SearchQuery(QueryType queryType, String query) {
        this.queryType = queryType;
        this.query = query;
    }

    public QueryType getQueryType() {
        return queryType;
    }

    public String getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return queryType + "{" + "query='" + query + '\'' + '}';
    }
}
