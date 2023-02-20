package com.insidious.plugin.ui.Components;

import com.insidious.plugin.client.ConstructorStrategy;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.ui.ParameterDataChangeListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.swing.*;
import java.util.List;

import static org.mockito.Mockito.*;

class ParameterEditorFormTest {
    @Mock
    Logger logger;
    @Mock
    Parameter parameter;
    @Mock
    List<ParameterDataChangeListener> listeners;
    @Mock
    PsiParameter psiParameter;
    @Mock
    JPanel mainContainer;
    @Mock
    JLabel parameterTypeLabel;
    @Mock
    JPanel parameterPanel;
    @Mock
    JComboBox<ConstructorStrategy> creatorStrategySelector;
    @Mock
    JPanel valueSelectorPanel;
    //Field constructorStrategy of type ConstructorStrategy - was not mocked since Mockito doesn't mock enums
    @InjectMocks
    ParameterEditorForm parameterEditorForm;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAddChangeListener() {
        parameterEditorForm.addChangeListener(null);
    }
}

//Generated with love by TestMe :) Please report issues and submit feature requests at: http://weirddev.com/forum#!/testme