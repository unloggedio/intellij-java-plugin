package com.insidious.plugin.ui.stomp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.assertions.Expression;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.util.AtomicAssertionUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.ObjectMapperInstance;
import com.intellij.openapi.diagnostic.Logger;
import org.apache.xmlbeans.impl.store.Cur;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class TestCandidateSaveForm {
    private final static Logger logger = LoggerUtil.getInstance(TestCandidateSaveForm.class);
    private final List<TestCandidateMetadata> candidateMetadataList;
    private final SaveFormListener saveFormListener;
    private final List<StoredCandidate> candidateList;
    private final ObjectMapper objectMapper = ObjectMapperInstance.getInstance();
    private JPanel mainPanel;
    private JButton saveButton;
    private JButton cancelButton;
    private JLabel assertionCountLabel;
    private JLabel linesCountLabel;
    private JLabel selectedReplayCountLabel;
    private JLabel mockCallCountLabel;
    private JRadioButton integrationRadioButton;
    private JRadioButton unitRadioButton;
    private JSeparator replaySummaryLine;
    private JLabel replayExpandIcon;
    private JPanel replayLineContainer;

    public TestCandidateSaveForm(List<TestCandidateMetadata> candidateMetadataList, SaveFormListener saveFormListener) {
        this.candidateMetadataList = candidateMetadataList;
        this.saveFormListener = saveFormListener;
        selectedReplayCountLabel.setText(candidateMetadataList.size() + " replay selected");

        long downstreamCallCount = candidateMetadataList.stream().mapToLong(e -> e.getCallsList().size()).sum();
        mockCallCountLabel.setText(downstreamCallCount + " downstream call mocks");

        replaySummaryLine.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        replayExpandIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        selectedReplayCountLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        replayLineContainer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        replaySummaryLine.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
            }
        });

        candidateList = candidateMetadataList.stream()
                .map(candidateMetadata -> {

                    JsonNode returnValue;
                    MethodCallExpression mainMethod = candidateMetadata.getMainMethod();
                    Parameter returnValue1 = mainMethod.getReturnValue();
                    String stringValue = returnValue1.getStringValue();
                    String nonNullReturnValue = stringValue == null ? "null" : stringValue;
                    try {
                        returnValue = objectMapper.readTree(nonNullReturnValue);
                    } catch (JsonProcessingException e) {
                        logger.warn("Failed to parse response value as a json object: " + e.getMessage());
                        returnValue =
                                objectMapper.getNodeFactory().textNode(nonNullReturnValue);
                    }
                    StoredCandidate storedCandidate = new StoredCandidate(candidateMetadata);


                    AtomicAssertion assertion = createAssertions(returnValue);
                    storedCandidate.setTestAssertions(assertion);
                    return storedCandidate;
                })
                .collect(Collectors.toList());

        long assertionCount = candidateList.stream()
                .map(e -> AtomicAssertionUtils.countAssertions(e.getTestAssertions()))
                .collect(Collectors.summarizingInt(e -> e)).getCount();

        assertionCountLabel.setText(assertionCount + " assertions");

        Set<String> lineCoverageMap = new HashSet<>();
        Set<String> classNames = new HashSet<>();
        for (StoredCandidate storedCandidate : candidateList) {
            List<Integer> lines = storedCandidate.getLineNumbers();
            String hashKey = storedCandidate.getMethod().getMethodHashKey() + "#" + lines;

            classNames.add(storedCandidate.getMethod().getClassName());
            lineCoverageMap.add(hashKey);
        }

        int classCount = classNames.size();
        linesCountLabel.setText(lineCoverageMap.size() + " unique lines covered in " + classCount + " class"
        + (classCount == 1 ? "" : "es"));


    }

    private AtomicAssertion createAssertions(JsonNode value, String key) {
        if (value.isArray()) {

            AtomicAssertion parentAssertion = new AtomicAssertion(Expression.SELF, AssertionType.ALLOF, key, null);


            int arrayObjectLength = value.size();
            for (int i = 0; i < arrayObjectLength; i++) {
                JsonNode arrayObject = value.get(i);
                AtomicAssertion subAssertions = createAssertions(arrayObject,
                        (Objects.equals(key, "/") ? "/" + i : key + "/" + i));
                parentAssertion.getSubAssertions().add(subAssertions);
            }
            return parentAssertion;

        } else if (value.isObject()) {
            Iterator<String> fields = value.fieldNames();
            AtomicAssertion parentAssertion = new AtomicAssertion(Expression.SELF, AssertionType.ALLOF, key, null);
            while (fields.hasNext()) {
                String field = fields.next();
                JsonNode fieldValue = value.get(field);
                AtomicAssertion subAssertions = createAssertions(fieldValue,
                        (Objects.equals(key, "/") ? "/" + field : key + "/" + field));
                parentAssertion.getSubAssertions().add(subAssertions);
            }

            return parentAssertion;
        } else {
            return new AtomicAssertion(Expression.SELF, AssertionType.EQUAL, key,
                    value.toString());
        }
    }

    private AtomicAssertion createAssertions(JsonNode returnValue) {
        return createAssertions(returnValue, "/");
    }

    public JPanel getComponent() {
        return mainPanel;
    }
}
