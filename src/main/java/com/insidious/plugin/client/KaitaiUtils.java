package com.insidious.plugin.client;

import com.insidious.common.parser.KaitaiInsidiousClassWeaveParser;
import com.insidious.common.weaver.*;
import org.jetbrains.annotations.NotNull;

public class KaitaiUtils {

    @NotNull
    public static DataInfo toDataInfo(KaitaiInsidiousClassWeaveParser.ProbeInfo e) {
        String descriptorValue = e.valueDescriptor().value();

        Descriptor valueDesc = Descriptor.Object;
        if (!descriptorValue.startsWith("L")) {
            valueDesc = Descriptor.get(descriptorValue);
        }
        return new DataInfo(Math.toIntExact(e.classId()), Math.toIntExact(e.methodId()),
                Math.toIntExact(e.dataId()),
                Math.toIntExact(e.lineNumber()),
                Math.toIntExact(e.instructionIndex()),
                EventType.valueOf(e.eventType().value()),
                valueDesc,
                e.attributes().value());
    }

    @NotNull
    public static ClassInfo toClassInfo(KaitaiInsidiousClassWeaveParser.ClassInfo classInfo) {
        return new ClassInfo((int) classInfo.classId(),
                classInfo.container().value(),
                classInfo.fileName().value(),
                classInfo.className().value(),
                LogLevel.valueOf(classInfo.logLevel().value()),
                classInfo.hash().value(),
                classInfo.classLoaderIdentifier().value());
    }

    @NotNull
    public static MethodInfo toMethodInfo(
            KaitaiInsidiousClassWeaveParser.MethodInfo e) {
        return new MethodInfo(
                (int) e.classId(),
                (int) e.methodId(),
                null,
                e.methodName().value(),
                e.methodDescriptor().value(),
                (int) e.access(),
                e.sourceFileName().value(),
                e.methodHash().value());
    }

}
