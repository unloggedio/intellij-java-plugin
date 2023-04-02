package com.insidious.plugin.ui.Components;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;

import javax.swing.*;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AgentResponseComponent {
    private JPanel mainPanel;
    private JPanel borderParent;
    private JPanel topPanel;
    private JPanel bottomPanel;
    private JPanel centerPanel;
    private JPanel bottomControlPanel;
    private JButton viewFullButton;
    private JTable mainTable;
    private JScrollPane scrollParent;
    TreeMap<String,String> differences = new TreeMap<>();
    private String oldResponse;
    private String agentResponse;

    public AgentResponseComponent(String oldResponse, String returnvalue) {
        this.oldResponse = oldResponse;
        this.agentResponse = returnvalue;
        //tryTestDiff();
        computeDifferences();
    }

    public JPanel getComponenet()
    {
        return this.mainPanel;
    }

    //constructor to take string difference and parameter/mce from candidate
    public void computeDifferences()
    {
        caluclateDifferences(this.oldResponse,this.agentResponse);
    }

    public void tryTestDiff()
    {
        //String s1 = "{\"indicate\":[{\"name\":\"c\",\"age\":24},\"doing\",\"brain\"],\"thousand\":false,\"number\":\"machine\"}";
        String s1 = "";
        String s2 = "{\"indicate\":[{\"name\":\"a\",\"age\":25},\"doing\",\"e\"],\"thousand\":false,\"number\":\"daboi\"}";
        caluclateDifferences(s1,s2);
    }

    private void caluclateDifferences(String s1, String s2)
    {
        ObjectMapper om = new ObjectMapper();
        try {
            Map<String, Object> m1;
            if(s1==null || s1.isEmpty())
            {
                m1 = new TreeMap<>();
            }
            else
            {
                m1 = (Map<String, Object>)(om.readValue(s1, Map.class));

            }
            Map<String, Object> m2 = (Map<String, Object>)(om.readValue(s2, Map.class));
            System.out.println("TestDiff : ");

            System.out.println("Differences : ");
            MapDifference<String,Object> res = Maps.difference(flatten(m1),flatten(m2));
            System.out.println(res);

            System.out.println("Left Entries");
            res.entriesOnlyOnLeft()
                    .forEach((key, value) -> System.out.println(key + ": " + value));
            Map<String,Object> leftOnly = res.entriesOnlyOnLeft();

            System.out.println("Right Entries");
            res.entriesOnlyOnRight()
                    .forEach((key, value) -> System.out.println(key + ": " + value));
            Map<String,Object> rightOnly = res.entriesOnlyOnRight();

            System.out.println("Differing entries");
            res.entriesDiffering()
                    .forEach((key, value) -> System.out.println(key + ": " + value));
            Map<String, MapDifference.ValueDifference<Object>> differences = res.entriesDiffering();
            if(m1.equals(m2))
            {
                //no differences
                Map<String,Object> same = new HashMap<>();
                same.put("No Differences","Both responses are the same");
            }
            else if(s1==null || s1.isEmpty())
            {
                renderTableForResponse(rightOnly);
            }
            else {
                //merge left and right differneces
                //or iterate and create a new pojo that works with 1 table model
                renderTableWithDifferences(differences);
            }
        } catch (Exception e) {
            System.out.println("TestDiff Exception: "+e);
            e.printStackTrace();
        }
    }

    private void renderTableWithDifferences(Map<String, MapDifference.ValueDifference<Object>> differences) {
        CompareTableModel newModel = new CompareTableModel(differences);
        this.mainTable.setModel(newModel);
        this.mainTable.revalidate();
    }

    private void renderTableForResponse(Map<String, Object> rightOnly)
    {
        ResponseMapTable newModel = new ResponseMapTable(rightOnly);
        this.mainTable.setModel(newModel);
        this.mainTable.revalidate();
    }

    public Map<String, Object> flatten(Map<String, Object> map) {
        return map.entrySet().stream()
                .flatMap(this::flatten)
                .collect(LinkedHashMap::new, (m, e) -> m.put("/" + e.getKey(), e.getValue()), LinkedHashMap::putAll);
    }

    private Stream<Map.Entry<String, Object>> flatten(Map.Entry<String, Object> entry) {
        if (entry == null) {
            return Stream.empty();
        }

        if (entry.getValue() instanceof Map<?, ?>) {
            return ((Map<?, ?>) entry.getValue()).entrySet().stream()
                    .flatMap(e -> flatten(new AbstractMap.SimpleEntry<>(entry.getKey() + "/" + e.getKey(), e.getValue())));
        }

        if (entry.getValue() instanceof List<?>) {
            List<?> list = (List<?>) entry.getValue();
            return IntStream.range(0, list.size())
                    .mapToObj(i -> new AbstractMap.SimpleEntry<String, Object>(entry.getKey() + "/" + i, list.get(i)))
                    .flatMap(this::flatten);
        }

        return Stream.of(entry);
    }
}
