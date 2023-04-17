package com.insidious.plugin.ui.methodscope;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.ui.Components.ResponseMapTable;
import com.insidious.plugin.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AgentExceptionResponseComponent {
    private final ObjectMapper objectMapper;
    private JPanel mainPanel;
    private JPanel defParent;
    private JPanel afterBorderParent;
    private JPanel beforeBorderParent;

    private InsidiousService insidiousService;

    private TestCandidateMetadata metadata;

    private AgentCommandResponse response;

    public AgentExceptionResponseComponent(TestCandidateMetadata metadata, AgentCommandResponse response, InsidiousService insidiousService) {

        this.insidiousService = insidiousService;
        this.objectMapper = insidiousService.getObjectMapper();
        this.metadata = metadata;
        this.response = response;

        setupDefLayout();
    }

    public void setupDefLayout()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

//        String value1 = "B1 Stack trace";
//            String value2 = "B2\n" +
//                    "b2 line2\n" +
//                    "File : b1.file\n" +
//                    "Line Number : 12";
//        ExceptionOptionsComponent options1 = new ExceptionOptionsComponent(value2,
//                value1, insidiousService);
//        panel.add(options1.getComponent());
//
//                    Map<String,String> value = new TreeMap<>();
//                    value.put("A","a");
//                    value.put("B","a");
//                    value.put("C","a");
//                    value.put("D","a");
//                    value.put("e","a");
//            String json = "{\"red\":false,\"student\":44,\"basis\":false,\"she\":66,\"story\":88,\"unless\":true}";
//            JTableComponent comp = new JTableComponent(getModelFor(json));
//                    panel.add(comp.getComponent());

        if (response.getResponseType() != null && (response.getResponseType().equals(ResponseType.EXCEPTION)
                || response.getResponseType().equals(ResponseType.FAILED))) {
            //load after as Exception.
            //loadAfterAsException();
            ExceptionOptionsComponent options;
            if (response.getResponseType().equals(ResponseType.EXCEPTION)) {
                if (response.getMethodReturnValue() != null) {
                    options = new ExceptionOptionsComponent(response.getMessage(),
                            ExceptionUtils.prettyPrintException(response.getMethodReturnValue()), insidiousService);
                } else {
                    options = new ExceptionOptionsComponent(response.getMessage(),
                            response.getMessage(), insidiousService);
                }
            } else {
                options = new ExceptionOptionsComponent(response.getMessage(),
                        String.valueOf(response.getMethodReturnValue()), insidiousService);
            }
            panel.add(options.getComponent());
        } else {
            //load after as normal response.
            //loadAfterAsNormal();
            String value = String.valueOf(response.getMethodReturnValue());
            JTableComponent comp = new JTableComponent(getModelFor(value));
            panel.add(comp.getComponent());
        }

        if (metadata.getMainMethod().getReturnValue().isException()) {
            //load Before As Exception.
            //loadBeforeAsException();
            ExceptionOptionsComponent options = new ExceptionOptionsComponent("Exception message",
                    ExceptionUtils.prettyPrintException(
                            metadata.getMainMethod().getReturnValue().getProb().getSerializedValue()), insidiousService);
            panel.add(options.getComponent());

        } else {
            //load before as normal response.
            //loadBeforeAsNormal();
            String value = String.valueOf(response.getMethodReturnValue());
            JTableComponent comp = new JTableComponent(getModelFor(value));
            panel.add(comp.getComponent());
        }

        this.defParent.add(panel, BorderLayout.CENTER);
        panel.revalidate();
        this.defParent.revalidate();
    }

    public JPanel getComponent() {
        return this.mainPanel;
    }

    public void loadAfterAsException() {
        this.afterBorderParent.removeAll();
        ExceptionOptionsComponent options;
        if (response.getResponseType().equals(ResponseType.EXCEPTION)) {
            if (response.getMethodReturnValue() != null) {
                options = new ExceptionOptionsComponent(response.getMessage(),
                        ExceptionUtils.prettyPrintException(response.getMethodReturnValue()), insidiousService);
            } else {
                options = new ExceptionOptionsComponent(response.getMessage(),
                        response.getMessage(), insidiousService);
            }
        } else {
            options = new ExceptionOptionsComponent(response.getMessage(),
                    String.valueOf(response.getMethodReturnValue()), insidiousService);
        }
        this.afterBorderParent.add(options.getComponent(), BorderLayout.CENTER);
        this.afterBorderParent.revalidate();
    }

    public void loadAfterAsNormal() {
        this.afterBorderParent.removeAll();
        String value = String.valueOf(response.getMethodReturnValue());
        JTableComponent comp = new JTableComponent(getModelFor(value));
        this.afterBorderParent.add(comp.getComponent(), BorderLayout.CENTER);
        this.afterBorderParent.revalidate();
    }

    public void loadBeforeAsException() {
        this.beforeBorderParent.removeAll();
        String value1 = metadata.getMainMethod().getReturnValue().getStringValue();
        String value2 = new String(metadata.getMainMethod().getReturnDataEvent().getSerializedValue());
        System.out.println("EXCEPTION BEFORE V1 : " + value1);
        System.out.println("EXCEPTION BEFORE V2 : " + value2);
        ExceptionOptionsComponent options = new ExceptionOptionsComponent("Exception message",
                ExceptionUtils.prettyPrintException(
                        metadata.getMainMethod().getReturnValue().getProb().getSerializedValue()), insidiousService);
        this.beforeBorderParent.add(options.getComponent(), BorderLayout.CENTER);
        this.beforeBorderParent.revalidate();
    }

    public void loadBeforeAsNormal() {
        this.beforeBorderParent.removeAll();
        String response = new String(metadata.getMainMethod().getReturnDataEvent().getSerializedValue());
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
            m1.put("value", s1);
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
