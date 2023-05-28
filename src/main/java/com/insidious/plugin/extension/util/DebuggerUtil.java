package com.insidious.plugin.extension.util;

import com.insidious.plugin.extension.InsidiousXSuspendContext;
import com.insidious.plugin.extension.connector.InsidiousJDIConnector;
import com.insidious.plugin.extension.evaluation.JVMNameUtil;
import com.insidious.plugin.extension.thread.InsidiousVirtualMachineProxy;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.requests.Requestor;
import com.intellij.debugger.settings.DebuggerSettings;
import com.intellij.debugger.ui.breakpoints.Breakpoint;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.classFilter.ClassFilter;
import com.intellij.ui.classFilter.DebuggerClassFilterProvider;
import com.intellij.util.SmartList;
import com.intellij.util.ThreeState;
import com.intellij.util.containers.ContainerUtil;
import com.sun.jdi.*;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.EventRequest;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DebuggerUtil {
    private static final Logger logger = LoggerUtil.getInstance(DebuggerUtil.class);


    public static boolean methodMatches(@NotNull Method m, @NotNull String name, @NotNull String signature) {
        return (name.equals(m.name()) && signature.equals(m.signature()));
    }


    public static boolean methodMatches(@NotNull PsiMethod psiMethod, String className, String name, String signature, InsidiousVirtualMachineProxy virtualMachineProxy) {
        PsiClass containingClass = psiMethod.getContainingClass();
        try {
            if (containingClass != null &&
                    JVMNameUtil.getJVMMethodName(psiMethod).equals(name) &&
                    JVMNameUtil.getJVMSignature(psiMethod)
                            .getName(virtualMachineProxy)
                            .equals(signature)) {
                String methodClassName = JVMNameUtil.getClassVMName(containingClass);
                if (Objects.equals(methodClassName, className)) {
                    return true;
                }
                if (methodClassName != null) {


                    boolean res = virtualMachineProxy.classesByName(className).stream().anyMatch(t -> DebuggerUtilsEx.instanceOf(t, methodClassName));
                    if (res) {
                        return true;
                    }

                    Project project = virtualMachineProxy.getXDebugProcess().getSession().getProject();

                    PsiClass aClass = findClass(project, className, GlobalSearchScope.allScope(project));
                    return (aClass != null && aClass.isInheritor(containingClass, true));
                }
            }
        } catch (EvaluateException e) {
            logger.debug("failed", e);
        }
        return false;
    }


    public static PsiClass findClass(Project project, String originalQName, GlobalSearchScope searchScope) {
        return ReadAction.compute(() -> {
            PsiClass psiClass = DebuggerUtils.findClass(originalQName, project, searchScope);
            if (psiClass == null) {
                int dollar = originalQName.indexOf('$');
                if (dollar > 0) {
                    psiClass = DebuggerUtils.findClass(originalQName.substring(0, dollar), project, searchScope);
                }
            }
            return psiClass;
        });
    }


    @NotNull
    public static List<Location> locationsOfLine(@NotNull Method method, int line) {
        try {
            return method.locationsOfLine("Java", null, line);
        } catch (AbsentInformationException absentInformationException) {
            return Collections.emptyList();
        }

    }

    @NotNull
    public static ThreeState getEffectiveAssertionStatus(@NotNull Location location) {
        ReferenceType type = location.declaringType();
        if (type instanceof com.sun.jdi.ClassType) {
            Field field = type.fieldByName("$assertionsDisabled");
            if (field != null && field.isStatic() && field.isSynthetic()) {
                Value value = type.getValue(field);
                if (value instanceof BooleanValue) {
                    return ThreeState.fromBoolean(!((BooleanValue) value).value());
                }
            }
        }
        return ThreeState.UNSURE;
    }

    public static boolean isVoid(@NotNull Method method) {
        return "void".equals(method.returnTypeName());
    }


    public static List<Pair<Breakpoint, Event>> getEventDescriptors(@Nullable InsidiousXSuspendContext suspendContext) {
        if (suspendContext != null) {
            EventSet events = suspendContext.getEventSet();
            if (!ContainerUtil.isEmpty(events)) {
                SmartList<Pair> smartList = new SmartList();
                for (Event event : events) {
                    EventRequest request = event.request();
                    Requestor requestor = null;
                    if (request != null) {
                        requestor = (Requestor) request.getProperty(InsidiousJDIConnector.REQUESTOR);
                    }
                    if (requestor instanceof Breakpoint) {
                        smartList.add(Pair.create(requestor, event));
                    }
                }
                return (List) smartList;
            }
        }
        return Collections.emptyList();
    }

    public static boolean isPositionFiltered(Location location) {
        List<ClassFilter> activeFilters = getActiveFilters();
        if (!activeFilters.isEmpty()) {
            ReferenceType referenceType = (location != null) ? location.declaringType() : null;
            if (referenceType != null) {
                String currentClassName = referenceType.name();
                return (currentClassName != null &&
                        DebuggerUtilsEx.isFiltered(currentClassName, activeFilters));
            }
        }
        return false;
    }

    @NotNull
    private static List<ClassFilter> getActiveFilters() {
        DebuggerSettings settings = DebuggerSettings.getInstance();


        StreamEx<ClassFilter> stream = StreamEx.of(DebuggerClassFilterProvider.EP_NAME.getExtensionList()).flatCollection(DebuggerClassFilterProvider::getFilters);
        if (settings.TRACING_FILTERS_ENABLED) {
            stream = stream.prepend(settings.getSteppingFilters());
        }
        return ((StreamEx) stream.filter(ClassFilter::isEnabled)).toList();
    }

    public static int getLineNumber(Location location, boolean zeroBased) {
        int lineNumber = -1;

        try {
            lineNumber = location.lineNumber();
        } catch (InternalError | IllegalArgumentException internalError) {
        }


        if (lineNumber == -1) {
            try {
                lineNumber = location.method().location().lineNumber();
            } catch (InternalError | IllegalArgumentException e) {
                return -1;
            }
        }

        return zeroBased ? --lineNumber : lineNumber;
    }

    public static boolean isKeyInputValid(String key) {
        return isKeyValid(stripUnwantedCharacters(key));
    }

    private static boolean isKeyValid(String key) {
        if (StringUtils.isNotBlank(key)) {
            return isBase64EncodedString(key);
        }
        return false;
    }

    public static boolean isKeyValid() {
        return isKeyValid(getLicenseKey());
    }


    public static String getLicenseKey() {
        return "KEY";
    }

    private static String stripUnwantedCharacters(String licenceKeyStr) {
        return StringUtils.isNotBlank(licenceKeyStr) ?
                stripWhitespace(stripInsidiousBlocks(licenceKeyStr)) :
                licenceKeyStr;
    }

    private static String stripWhitespace(String licenceKeyStr) {
        return licenceKeyStr.replaceAll("\\s+", "");
    }

    private static String stripInsidiousBlocks(String licenceKeyStr) {
        StringBuilder licenceBuilder = new StringBuilder();
        licenceKeyStr
                .lines()
                .forEach(line -> {
                    if (!line.contains("Insidious KEY"))
                        licenceBuilder.append(line);
                });
        return licenceBuilder.toString();
    }

    private static boolean isBase64EncodedString(String licenceKeyStr) {
        try {
            Base64.getDecoder().decode(licenceKeyStr);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }
}


