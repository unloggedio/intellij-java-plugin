package com.insidious.plugin.client;

import com.insidious.common.parser.KaitaiInsidiousClassWeaveParser;
import com.insidious.common.weaver.*;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

public class KaitaiUtils {

    private final static Descriptor[] DescriptorValues = Descriptor.values();

    @NotNull
    public static DataInfo toDataInfo(KaitaiInsidiousClassWeaveParser.ProbeInfo e) {
//        String descriptorValue = DescriptorValues[e.valueDescriptor().ordinal()].getString();

        Descriptor valueDesc = DescriptorValues[e.valueDescriptor().ordinal()];
//        if (!descriptorValue.startsWith("L")) {
//            valueDesc = Descriptor.get(descriptorValue);
//        }
        long lineNumber = e.lineNumber();

        EventType eventType = EventType.valueOf(e.eventType().name());
        long instructionIndex = e.instructionIndex();
        if (eventType.equals(EventType.RESERVED)) {
            lineNumber = -1;
            instructionIndex = -1;
        }

        return new DataInfo(Math.toIntExact(e.classId()), Math.toIntExact(e.methodId()),
                Math.toIntExact(e.dataId()),
                Math.toIntExact(lineNumber),
                Math.toIntExact(instructionIndex),
                eventType,
                valueDesc,
                e.attributes().value());
    }


    @NotNull
    public static ProbeInfoDocument toProbeInfoDocument(KaitaiInsidiousClassWeaveParser.ProbeInfo e) {
//        String descriptorValue = DescriptorValues[e.valueDescriptor().ordinal()].getString();

        Descriptor valueDesc = DescriptorValues[e.valueDescriptor().ordinal()];
//        if (!descriptorValue.startsWith("L")) {
//            valueDesc = Descriptor.get(descriptorValue);
//        }
        long lineNumber = e.lineNumber();

        EventType eventType = EventType.valueOf(e.eventType().name());
        long instructionIndex = e.instructionIndex();
        if (eventType.equals(EventType.RESERVED)) {
            lineNumber = -1;
            instructionIndex = -1;
        }

        ProbeInfoDocument probeInfoDocument = new ProbeInfoDocument();
        probeInfoDocument.setAttributes(e.attributes().value());
        probeInfoDocument.setClassId(Math.toIntExact(e.classId()));
        probeInfoDocument.setDataId(Math.toIntExact(e.dataId()));
        probeInfoDocument.setLine(Math.toIntExact(lineNumber));
        probeInfoDocument.setEventType(eventType);
        probeInfoDocument.setMethodId(Math.toIntExact(e.methodId()));
        probeInfoDocument.setValueDesc(valueDesc);
        probeInfoDocument.setInstructionIndex(Math.toIntExact(instructionIndex));
        return probeInfoDocument;
    }


    @NotNull
    public static ClassInfo toClassInfo(KaitaiInsidiousClassWeaveParser.ClassInfo classInfo) {
        String[] interfaceList = classInfo.interfaceNames()
                .stream()
                .map(KaitaiInsidiousClassWeaveParser.StrWithLen::value)
                .collect(Collectors.toList())
                .toArray(new String[]{});
        return new ClassInfo((int) classInfo.classId(),
                classInfo.container().value(),
                classInfo.fileName().value(),
                classInfo.className().value(),
                LogLevel.valueOf(classInfo.logLevel().value()),
                classInfo.hash().value(),
                classInfo.classLoaderIdentifier().value(),
                interfaceList,
                classInfo.superclass().value(),
                classInfo.signature().value()
        );
    }

    @NotNull
    public static MethodInfo toMethodInfo(
            KaitaiInsidiousClassWeaveParser.MethodInfo e, String className) {
        return new MethodInfo(
                (int) e.classId(),
                (int) e.methodId(),
                className,
                e.methodName().value(),
                e.methodDescriptor().value(),
                (int) e.access(),
                e.sourceFileName().value(),
                e.methodHash().value());
    }

}
