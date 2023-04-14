package com.insidious.plugin.ui.methodscope;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.ui.Components.ResponseMapTable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AgentExceptionResponseComponent {
    private JPanel mainPanel;
    private JPanel afterSection;
    private JPanel beforeSection;
    private JPanel afterBorderParent;
    private JPanel beforeBorderParent;

    private InsidiousService insidiousService;

    private TestCandidateMetadata metadata;

    private AgentCommandResponse response;

    public JPanel getComponent() {
        return this.mainPanel;
    }

    public AgentExceptionResponseComponent(TestCandidateMetadata metadata, AgentCommandResponse response, InsidiousService insidiousService) {

        this.insidiousService = insidiousService;
        this.metadata = metadata;
        this.response = response;

        if(response.getResponseType()!=null && (response.getResponseType().equals(ResponseType.EXCEPTION)
                || response.getResponseType().equals(ResponseType.FAILED)))
        {
            //load after as Exception.
            loadAfterAsException();
        }
        else
        {
            //load after as notmal response.
            loadAfterAsNormal();
        }

        if(metadata.getMainMethod().getReturnValue().isException())
        {
            //load Before As Exception.
            loadBeforeAsException();
        }
        else
        {
            //load before as normal response.
            loadBeforeAsNormal();
        }

    }

    public void loadAfterAsException()
    {
        this.afterBorderParent.removeAll();
        System.out.println("EXCEPTION AFTER MSG : "+response.getMessage());
        System.out.println("EXCEPTION AFTER DATA : "+response.getMethodReturnValue());
        ExceptionOptionsComponent options = new ExceptionOptionsComponent(response.getMessage(),
                String.valueOf(response.getMethodReturnValue()),insidiousService);
        this.afterBorderParent.add(options.getComponent(), BorderLayout.CENTER);
        this.afterBorderParent.revalidate();
    }

    public void loadAfterAsNormal()
    {
        this.afterBorderParent.removeAll();
        String value = String.valueOf(response.getMethodReturnValue());
        JTableComponent comp =
                new JTableComponent(getModelFor(value));
        this.afterBorderParent.add(comp.getComponent(), BorderLayout.CENTER);
        this.afterBorderParent.revalidate();
    }

    public void loadBeforeAsException()
    {
        this.beforeBorderParent.removeAll();
        String value1 = metadata.getMainMethod().getReturnValue().getStringValue();
        String value2 = new String(metadata.getMainMethod().getReturnDataEvent().getSerializedValue());
        System.out.println("EXCEPTION BEFORE V1 : "+value1);
        System.out.println("EXCEPTION BEFORE V2 : "+value2);
        ExceptionOptionsComponent options = new ExceptionOptionsComponent("Exception message",
                String.valueOf(metadata.getMainMethod().getReturnValue().getStringValue()),insidiousService);
        this.beforeBorderParent.add(options.getComponent(), BorderLayout.CENTER);
        this.beforeBorderParent.revalidate();
    }

    public void loadBeforeAsNormal()
    {
        this.beforeBorderParent.removeAll();
        String response = new String(metadata.getMainMethod().getReturnDataEvent()
                .getSerializedValue());
        JTableComponent comp = new JTableComponent(getModelFor(response));
        this.beforeBorderParent.add(comp.getComponent(), BorderLayout.CENTER);
        this.beforeBorderParent.revalidate();
    }

    private ResponseMapTable getModelFor(String s1) {
        ObjectMapper om = new ObjectMapper();
        try {
            Map<String, Object> m1;
            if (s1 == null || s1.isEmpty()) {
                m1 = new TreeMap<>();
            } else {
                m1 = (Map<String, Object>) (om.readValue(s1, Map.class));
                m1 = flatten(m1);
            }
            return new ResponseMapTable(m1);
        } catch (Exception e) {
            System.out.println("Model make Exception: " + e);
            e.printStackTrace();
            Map<String, Object> m1 = new TreeMap<>();
            m1.put("value",s1);
            return new ResponseMapTable(m1);
        }
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
                    .flatMap(e -> flatten(
                            new AbstractMap.SimpleEntry<>(entry.getKey() + "/" + e.getKey(), e.getValue())));
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
