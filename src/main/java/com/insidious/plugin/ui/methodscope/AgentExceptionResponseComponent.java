package com.insidious.plugin.ui.methodscope;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.Components.ResponseMapTable;
import com.insidious.plugin.util.DateUtils;
import com.insidious.plugin.util.ExceptionUtils;
import com.insidious.plugin.util.JsonTreeUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.IOException;
import java.util.Date;

public class AgentExceptionResponseComponent implements ResponsePreviewComponent {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerUtil.getInstance(AgentExceptionResponseComponent.class);
    final private InsidiousService insidiousService;
    final private AgentCommandResponse<String> response;
    private final CandidateLifeListener candidateLifeListener;
    private StoredCandidate testCandidate;
    private JPanel mainPanel;
    private JPanel contentPanel;
    private JPanel afterSection;
    private JPanel beforeSection;

    public AgentExceptionResponseComponent(
            StoredCandidate testCandidate,
            AgentCommandResponse<String> agentCommandResponse,
            InsidiousService insidiousService,
            CandidateLifeListener candidateLifeListener
    ) {

        this.insidiousService = insidiousService;
        this.candidateLifeListener = candidateLifeListener;
        this.testCandidate = testCandidate;
        this.response = agentCommandResponse;
        setupDefLayout();
        String simpleClassName = testCandidate.getReturnValueClassname();
        if (simpleClassName != null && simpleClassName.contains(".")) {
            simpleClassName = simpleClassName.substring(simpleClassName.lastIndexOf(".") + 1);
        }
        String methodLabel = simpleClassName + "." + testCandidate.getMethod().getName() + "()";
        setInfoLabel(methodLabel + " at " + DateUtils.formatDate(new Date(agentCommandResponse.getTimestamp())));
    }


    public void setupDefLayout() {
        final String originalString = testCandidate.getReturnValue();
        final String actualString = String.valueOf(response.getMethodReturnValue());
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
            options = new ExceptionPreviewComponent(responseMessage, stacktrace, insidiousService,
                    candidateLifeListener, true, false, testCandidate, response);
            options.setBorderTitle("After");
            JPanel component = options.getComponent();
            afterSection.add(component, BorderLayout.CENTER);
        } else {
            JTableComponent comp = new JTableComponent(getModelFor(actualString));
            comp.setBorderTitle("After");
            afterSection.add(comp.getComponent(), BorderLayout.CENTER);
        }

        if (testCandidate.isException()) {
            String prettyException = ExceptionUtils.prettyPrintException(testCandidate.getProbSerializedValue());
            String exceptionMessage = "Exception message";
            try {
                JsonNode jsonNode = objectMapper.readValue(testCandidate.getProbSerializedValue(), JsonNode.class);
                if (jsonNode.has("message")) {
                    exceptionMessage = jsonNode.get("message").asText();
                }
            } catch (IOException e) {
                // failed to read return value as json node
            }
            boolean showDelete = testCandidate.getCandidateId() != null;
            ExceptionPreviewComponent options = new ExceptionPreviewComponent(exceptionMessage,
                    prettyException, insidiousService, candidateLifeListener, false, showDelete, testCandidate,
                    response);
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
        try {
            ObjectNode m1;
            if (s1 == null || s1.isEmpty()) {
                m1 = objectMapper.createObjectNode();
            } else {
                m1 = JsonTreeUtils.flatten(objectMapper.readTree(s1));
            }
            return new ResponseMapTable(m1);
        } catch (Exception e) {
            logger.warn("Model make Exception: ", e);
            e.printStackTrace();
            ObjectNode newObjectNode = objectMapper.createObjectNode();
            newObjectNode.put("value", s1);
            return new ResponseMapTable(newObjectNode);
        }
    }

//    public Map<String, Object> flatten(Map<String, Object> map) {
//        return map.entrySet().stream()
//                .flatMap(this::flatten)
//                .collect(LinkedHashMap::new, (m, e) -> m.put("/" + e.getKey(), e.getValue()), LinkedHashMap::putAll);
//    }


//    private Stream<Map.Entry<String, Object>> flatten(Map.Entry<String, Object> entry) {
//        if (entry == null) {
//            return Stream.empty();
//        }
//
//        if (entry.getValue() instanceof Map<?, ?>) {
//            return ((Map<?, ?>) entry.getValue()).entrySet().stream()
//                    .flatMap(e -> flatten(
//                            new AbstractMap.SimpleEntry<>(entry.getKey() + "/" + e.getKey(), e.getValue())));
//        }
//
//        if (entry.getValue() instanceof List<?>) {
//            List<?> list = (List<?>) entry.getValue();
//            return IntStream.range(0, list.size())
//                    .mapToObj(i -> new AbstractMap.SimpleEntry<String, Object>(entry.getKey() + "/" + i, list.get(i)))
//                    .flatMap(this::flatten);
//        }
//
//        return Stream.of(entry);
//    }

    @Override
    public Component get() {
        return this.mainPanel;
    }

    @Override
    public void setTestCandidate(StoredCandidate candidate) {
        this.testCandidate = candidate;
    }

    @Override
    public StoredCandidate getTestCandidate() {
        return testCandidate;
    }

    public void setInfoLabel(String info) {
        TitledBorder titledBorder = (TitledBorder) mainPanel.getBorder();
        titledBorder.setTitle(info);
//        this.infoLabel.setText(info);
    }

}
