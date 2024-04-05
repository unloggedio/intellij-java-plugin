package com.insidious.plugin.client;

import com.insidious.common.parser.KaitaiInsidiousClassWeaveParser;
import com.insidious.common.weaver.*;
import com.insidious.plugin.pojo.dao.ProbeInfo;


import java.util.stream.Collectors;

public class KaitaiUtils {

    private final static Descriptor[] DescriptorValues = Descriptor.values();


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

        int intExact = 0;
        try {

            intExact = Math.toIntExact(lineNumber);
        }catch (ArithmeticException arithmeticException) {
            arithmeticException.printStackTrace();
        }
        return new DataInfo(Math.toIntExact(e.classId()), Math.toIntExact(e.methodId()),
                Math.toIntExact(e.dataId()),
                intExact,
                Math.toIntExact(instructionIndex),
                eventType,
                valueDesc,
                e.attributes().value());
    }


    public static DataInfo toDataInfo(ProbeInfo probeInfo) {
//        String descriptorValue = DescriptorValues[e.valueDescriptor().ordinal()].getString();

        Descriptor valueDesc = DescriptorValues[probeInfo.getValueDesc().ordinal()];
//        if (!descriptorValue.startsWith("L")) {
//            valueDesc = Descriptor.get(descriptorValue);
//        }
        long lineNumber = probeInfo.getLine();

        EventType eventType = EventType.valueOf(probeInfo.getEventType().name());
        long instructionIndex = probeInfo.getInstructionIndex();
        if (eventType.equals(EventType.RESERVED)) {
            lineNumber = -1;
            instructionIndex = -1;
        }

        int intExact = 0;
        try {

            intExact = Math.toIntExact(lineNumber);
        }catch (ArithmeticException arithmeticException) {
            arithmeticException.printStackTrace();
        }
        return new DataInfo(Math.toIntExact(probeInfo.getClassId()), Math.toIntExact(probeInfo.getMethodId()),
                Math.toIntExact(probeInfo.getProbeId()),
                intExact,
                Math.toIntExact(instructionIndex),
                eventType,
                valueDesc,
                probeInfo.getAttributes());
    }


//    
//    public static ProbeInfoDocument toProbeInfoDocument(KaitaiInsidiousClassWeaveParser.ProbeInfo e) {
////        String descriptorValue = DescriptorValues[e.valueDescriptor().ordinal()].getString();
//
//        Descriptor valueDesc = DescriptorValues[e.valueDescriptor().ordinal()];
////        if (!descriptorValue.startsWith("L")) {
////            valueDesc = Descriptor.get(descriptorValue);
////        }
//        long lineNumber = e.lineNumber();
//
//        EventType eventType = EventType.valueOf(e.eventType().name());
//        long instructionIndex = e.instructionIndex();
//        if (eventType.equals(EventType.RESERVED)) {
//            lineNumber = -1;
//            instructionIndex = -1;
//        }
//
//        ProbeInfoDocument probeInfoDocument = new ProbeInfoDocument();
//        probeInfoDocument.setAttributes(e.attributes().value());
//        probeInfoDocument.setClassId(Math.toIntExact(e.classId()));
//        probeInfoDocument.setDataId(Math.toIntExact(e.dataId()));
//        probeInfoDocument.setLine(Math.toIntExact(lineNumber));
//        probeInfoDocument.setEventType(eventType);
//        probeInfoDocument.setMethodId(Math.toIntExact(e.methodId()));
//        probeInfoDocument.setValueDesc(valueDesc);
//        probeInfoDocument.setInstructionIndex(Math.toIntExact(instructionIndex));
//        return probeInfoDocument;
//    }



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
