package com.insidious.plugin.ui.methodscope;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.pojo.atomic.StoredCandidateMetadata;
import com.insidious.plugin.ui.Components.AtomicRecord.AtomicRecordListener;
import com.insidious.plugin.ui.Components.ResponseMapTable;
import com.insidious.plugin.util.AtomicRecordUtils;
import com.insidious.plugin.util.DateUtils;
import com.insidious.plugin.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.insidious.plugin.factory.InsidiousService.HOSTNAME;

public class AgentExceptionResponseComponent implements Supplier<Component>, AtomicRecordListener {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    final private InsidiousService insidiousService;
    final private StoredCandidate metadata;
    final private AgentCommandResponse<String> response;
    private JPanel mainPanel;
    private JPanel contentPanel;
    private JPanel afterSection;
    private JPanel beforeSection;
    private String methodHash;
    private String methodName;
    private String classname;
    private String methodSignature;

    public AgentExceptionResponseComponent(
            StoredCandidate metadata,
            AgentCommandResponse<String> agentCommandResponse,
            InsidiousService insidiousService
    ) {

        this.insidiousService = insidiousService;
        this.metadata = metadata;
        this.response = agentCommandResponse;
        setupDefLayout();
        String simpleClassName = metadata.getReturnValueClassname();
        simpleClassName = simpleClassName.substring(simpleClassName.lastIndexOf(".") + 1);
        String methodLabel = simpleClassName + "." + metadata.getMethodName() + "()";
        setInfoLabel(methodLabel + " at " + DateUtils.formatDate(new Date(agentCommandResponse.getTimestamp())));
    }


    public void setupDefLayout() {
        final byte[] mainMethodReturnValue = metadata.getReturnDataEventSerializedValue().getBytes();
        final String originalString = new String(mainMethodReturnValue);
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
                    this,true,false,metadata);
            options.setBorderTitle("After");
            JPanel component = options.getComponent();
            afterSection.add(component, BorderLayout.CENTER);
        } else {
            JTableComponent comp = new JTableComponent(getModelFor(actualString));
            comp.setBorderTitle("After");
            afterSection.add(comp.getComponent(), BorderLayout.CENTER);
        }

        if (metadata.isException()) {
            String prettyException = ExceptionUtils.prettyPrintException(metadata.getProbSerializedValue());
            String exceptionMessage = "Exception message";
            try {
                JsonNode jsonNode = objectMapper.readValue(metadata.getProbSerializedValue(), JsonNode.class);
                if (jsonNode.has("message")) {
                    exceptionMessage = jsonNode.get("message").asText();
                }
            } catch (IOException e) {
                // failed to read return value as json node
            }
            boolean showDelete = false;
            if(metadata.getCandidateId()!=null)
            {
                showDelete = true;
            }
            ExceptionPreviewComponent options = new ExceptionPreviewComponent(exceptionMessage,
                    prettyException, insidiousService,this,false, showDelete, metadata);
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

    public void setInfoLabel(String info) {
        TitledBorder titledBorder = (TitledBorder) mainPanel.getBorder();
        titledBorder.setTitle(info);
//        this.infoLabel.setText(info);
    }

    public void setMethodHash(String methodHash) {
        this.methodHash = methodHash;
    }

    @Override
    public void triggerRecordAddition(String name, String description, StoredCandidate.AssertionType type) {
        this.metadata.setMethodHash(methodHash);
        StoredCandidate candidate = AtomicRecordUtils.createCandidateFor(metadata,response);
        candidate.setName(name);
        candidate.setDescription(description);
        candidate.setAssertionType(type);
        insidiousService.getAtomicRecordService().addStoredCandidate(classname,methodName,methodSignature,candidate);
    }

    @Override
    public void deleteCandidateRecord() {
        if(metadata.getCandidateId()!=null)
        {
            insidiousService.getAtomicRecordService().deleteStoredCandidate(classname,
                    methodName+"#"+methodSignature, metadata.getCandidateId());
        }
    }

    @Override
    public String getSaveLocation() {
        return insidiousService.getAtomicRecordService().getSaveLocation();
    }

    public void setMethodName(String name) {
        this.methodName = name;
    }

    public void setClassname(String qualifiedName) {
        this.classname = qualifiedName;
    }

    public void setMethodSignature(String jvmSignature) {
        this.methodSignature = jvmSignature;
    }
}
