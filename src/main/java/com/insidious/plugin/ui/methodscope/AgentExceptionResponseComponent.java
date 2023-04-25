package com.insidious.plugin.ui.methodscope;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.ui.Components.ResponseMapTable;
import com.insidious.plugin.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AgentExceptionResponseComponent implements Supplier<Component> {
    final private InsidiousService insidiousService;
    final private TestCandidateMetadata metadata;
    final private AgentCommandResponse<String> response;
    private JPanel mainPanel;
    private JPanel contentPanel;
    private JPanel afterSection;
    private JPanel beforeSection;
    private JLabel infoLabel;

    public AgentExceptionResponseComponent(
            TestCandidateMetadata metadata,
            AgentCommandResponse<String> response,
            InsidiousService insidiousService
    ) {

        this.insidiousService = insidiousService;
        this.metadata = metadata;
        this.response = response;
        setupDefLayout();
    }

    public void setupDefLayout() {

        final byte[] mainMethodReturnValue = metadata.getMainMethod().getReturnDataEvent().getSerializedValue();
        final String originalString = new String(mainMethodReturnValue);
        final String actualString = String.valueOf(response.getMethodReturnValue());
        System.out.println("EXCEPTION COMP DATA : ");
        System.out.println("BEFORE DATA : " + originalString);
        System.out.println("AFTER DATA : " + actualString);
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 1));
        afterSection.removeAll();
        beforeSection.removeAll();

        Object methodReturnValue = response.getMethodReturnValue();
        if (response.getResponseType() != null && (response.getResponseType().equals(ResponseType.EXCEPTION)
                || response.getResponseType().equals(ResponseType.FAILED))) {
            ExceptionPreviewComponent options;
            String responseMessage = response.getMessage();
            String stacktrace = responseMessage;
            if (response.getResponseType().equals(ResponseType.EXCEPTION)) {
                if (methodReturnValue != null) {
                    stacktrace = ExceptionUtils.prettyPrintException(actualString);
                }
            } else {
                stacktrace = String.valueOf(actualString);
            }
            options = new ExceptionPreviewComponent(responseMessage, stacktrace, insidiousService);
            options.setBorderTitle("After");
            JPanel component = options.getComponent();
            afterSection.add(component, BorderLayout.CENTER);
        } else {
            JTableComponent comp = new JTableComponent(getModelFor(actualString));
            comp.setBorderTitle("After");
            afterSection.add(comp.getComponent(), BorderLayout.CENTER);
        }

        Parameter returnValue = metadata.getMainMethod().getReturnValue();
        if (returnValue.isException()) {
            String prettyException = ExceptionUtils.prettyPrintException(returnValue.getProb().getSerializedValue());
            ExceptionPreviewComponent options = new ExceptionPreviewComponent("Exception message",
                    prettyException, insidiousService);
            options.setBorderTitle("Before");
            beforeSection.add(options.getComponent(), BorderLayout.CENTER);
        } else {
            JTableComponent comp = new JTableComponent(getModelFor(originalString));
            comp.setBorderTitle("Before");
            beforeSection.add(comp.getComponent(), BorderLayout.CENTER);
        }

        afterSection.revalidate();
        beforeSection.revalidate();
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

    @Override
    public Component get() {
        return this.mainPanel;
    }

    public void setInfoLabel(String info)
    {
        this.infoLabel.setText(info);
    }
}
