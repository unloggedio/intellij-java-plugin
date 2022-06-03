package com.insidious.plugin.extension.evaluation.expression;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.sun.jdi.*;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.EvaluatorUtil;

import java.util.ArrayList;
import java.util.Arrays;

class NewArrayInstanceEvaluator implements Evaluator {
    private static final Logger logger = LoggerUtil.getInstance(NewArrayInstanceEvaluator.class);

    private final Evaluator myArrayTypeEvaluator;

    private final Evaluator myDimensionEvaluator;

    private final Evaluator myInitializerEvaluator;


    NewArrayInstanceEvaluator(Evaluator arrayTypeEvaluator, Evaluator dimensionEvaluator, Evaluator initializerEvaluator) {
        this.myArrayTypeEvaluator = arrayTypeEvaluator;
        this.myDimensionEvaluator = dimensionEvaluator;
        this.myInitializerEvaluator = initializerEvaluator;
    }

    private static void setInitialValues(ArrayReference arrayReference, Object[] values, EvaluationContext context) throws EvaluateException {
        ArrayType type = (ArrayType) arrayReference.referenceType();
        try {
            if (type.componentType() instanceof ArrayType) {
                ArrayType componentType = (ArrayType) type.componentType();
                int length = arrayReference.length();
                for (int idx = 0; idx < length; idx++) {
                    Object value = values[idx];
                    if (value instanceof Value) {
                        arrayReference.setValue(idx, (Value) value);
                    } else {

                        ArrayReference componentArray = (ArrayReference) arrayReference.getValue(idx);
                        Object[] componentArrayValues = (Object[]) value;
                        if (componentArray == null) {

                            componentArray = EvaluatorUtil.newArrayInstance(componentType, componentArrayValues.length);

                            arrayReference.setValue(idx, componentArray);
                        }
                        setInitialValues(componentArray, componentArrayValues, context);
                    }

                }
            } else if (values.length > 0) {
                arrayReference.setValues(new ArrayList(Arrays.asList(values)));
            }

        } catch (ClassNotLoadedException ex) {
            ReferenceType referenceType;


            try {
                referenceType = context.isAutoLoadClasses() ? EvaluatorUtil.loadClass(context, ex.className(), type.classLoader()) : null;
            } catch (InvocationException | InvalidTypeException | com.sun.jdi.IncompatibleThreadStateException | ClassNotLoadedException e) {


                throw EvaluateExceptionUtil.createEvaluateException(e);
            }
            if (referenceType != null) {
                setInitialValues(arrayReference, values, context);
            } else {
                throw EvaluateExceptionUtil.createEvaluateException(
                        DebuggerBundle.message("error.class.not.loaded", ex.className()));
            }
        } catch (InvalidTypeException ex) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.incompatible.array.initializer.type"));
        } catch (IndexOutOfBoundsException ex) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.invalid.array.size"));
        } catch (ClassCastException ex) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.cannot.initialize.array"));
        }
    }

    public Object evaluate(EvaluationContext context) throws EvaluateException {
        int dimension;
        Object obj = this.myArrayTypeEvaluator.evaluate(context);
        if (!(obj instanceof ArrayType)) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.array.type.expected"));
        }
        ArrayType arrayType = (ArrayType) obj;

        Object[] initialValues = null;
        if (this.myDimensionEvaluator != null) {
            Object o = this.myDimensionEvaluator.evaluate(context);
            if (!(o instanceof Value) || !DebuggerUtils.isNumeric((Value) o)) {
                throw EvaluateExceptionUtil.createEvaluateException(
                        DebuggerBundle.message("evaluation.error.array.dimention.numeric.value.expected"));
            }

            PrimitiveValue value = (PrimitiveValue) o;
            dimension = value.intValue();
        } else {
            Object o = this.myInitializerEvaluator.evaluate(context);
            if (!(o instanceof Object[])) {
                throw EvaluateExceptionUtil.createEvaluateException(
                        DebuggerBundle.message("evaluation.error.cannot.evaluate.array.initializer"));
            }

            initialValues = (Object[]) o;
            dimension = initialValues.length;
        }
        ArrayReference arrayReference = EvaluatorUtil.newArrayInstance(arrayType, dimension);
        if (initialValues != null && initialValues.length > 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("Setting initial values: dimension = " + dimension + "; array size is " + initialValues.length);
            }


            setInitialValues(arrayReference, initialValues, context);
        }
        return arrayReference;
    }
}


