package com.insidious.plugin.extension.util;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.util.Computable;
import com.sun.jdi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class DebuggerUtilsAsync {
    private static final Logger LOG = LoggerUtil.getInstance(DebuggerUtilsAsync.class);


    public static CompletableFuture<String> getStringValue(StringReference value) {
        return CompletableFuture.completedFuture(value.value());
    }

    public static CompletableFuture<List<Field>> allFields(ReferenceType type) {
        return CompletableFuture.completedFuture(type.allFields());
    }

    public static CompletableFuture<List<Field>> fields(ReferenceType type) {
        return CompletableFuture.completedFuture(type.fields());
    }

    public static CompletableFuture<? extends Type> type(@Nullable Value value) {
        if (value == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.completedFuture(value.type());
    }

    public static CompletableFuture<Value> getValue(ObjectReference ref, Field field) {
        return CompletableFuture.completedFuture(ref.getValue(field));
    }


    public static CompletableFuture<Map<Field, Value>> getValues(ObjectReference ref, List<Field> fields) {
        return CompletableFuture.completedFuture(ref.getValues(fields));
    }


    public static CompletableFuture<Map<Field, Value>> getValues(ReferenceType type, List<Field> fields) {
        return CompletableFuture.completedFuture(type.getValues(fields));
    }


    public static CompletableFuture<List<Value>> getValues(ArrayReference ref, int index, int length) {
        return CompletableFuture.completedFuture(ref.getValues(index, length));
    }

    public static CompletableFuture<Integer> length(ArrayReference ref) {
        return CompletableFuture.completedFuture(Integer.valueOf(ref.length()));
    }


//    private static CompletableFuture<Boolean> instanceOf(@Nullable Type subType, @NotNull String superType) {
//        if (subType == null || subType instanceof com.sun.jdi.VoidType) {
//            return CompletableFuture.completedFuture(Boolean.valueOf(false));
//        }
//
//        if (subType instanceof com.sun.jdi.PrimitiveType) {
//            return CompletableFuture.completedFuture(Boolean.valueOf(superType.equals(subType.name())));
//        }
//
//        if ("java.lang.Object".equals(superType)) {
//            return CompletableFuture.completedFuture(Boolean.valueOf(true));
//        }
//
//        CompletableFuture<Boolean> res = new CompletableFuture<>();
//        instanceOfObject(subType, superType, res).thenRun(() -> res.complete(Boolean.valueOf(false)));
//
//        return res;
//    }


//    private static CompletableFuture<Void> instanceOfObject(@Nullable Type subType, @NotNull String superType, CompletableFuture<Boolean> res) {
//        if (subType == null || res.isDone()) {
//            return CompletableFuture.completedFuture(null);
//        }
//
//        if (typeEquals(subType, superType)) {
//            res.complete(Boolean.valueOf(true));
//            return CompletableFuture.completedFuture(null);
//        }
//
//        if (subType instanceof ClassType) {
//            return CompletableFuture.allOf(new CompletableFuture[]{
//                    superclass((ClassType) subType)
//                            .thenCompose(s -> instanceOfObject(s, superType, res)),
//                    interfaces((ClassType) subType)
//                            .thenCompose(interfaces -> CompletableFuture.allOf(interfaces.stream().map(()).toArray(())))
//            });
//        }
//
//
//        if (subType instanceof InterfaceType) {
//            return CompletableFuture.allOf(new CompletableFuture[]{
//                    superinterfaces((InterfaceType) subType)
//                            .thenCompose(interfaces -> CompletableFuture.allOf(interfaces.stream().map(()).toArray(())))
//            });
//        }
//
//
//        if (subType instanceof ArrayType && superType.endsWith("[]")) {
//            try {
//                String superTypeItem = superType.substring(0, superType.length() - 2);
//                Type subTypeItem = ((ArrayType) subType).componentType();
//                return instanceOf(subTypeItem, superTypeItem)
//                        .thenAccept(r -> {
//                            if (r.booleanValue())
//                                res.complete(Boolean.valueOf(true));
//                        });
//            } catch (ClassNotLoadedException classNotLoadedException) {
//            }
//        }
//
//
//        return CompletableFuture.completedFuture(null);
//    }


    private static boolean typeEquals(@NotNull Type type, @NotNull String typeName) {
        int genericPos = typeName.indexOf('<');
        if (genericPos > -1) {
            typeName = typeName.substring(0, genericPos);
        }
        return type.name().replace('$', '.').equals(typeName.replace('$', '.'));
    }


    public static CompletableFuture<Type> findAnyBaseType(@NotNull Type subType, Function<? super Type, ? extends CompletableFuture<Boolean>> checker) {
        CompletableFuture<Type> res = new CompletableFuture<>();
        findAnyBaseType(subType, checker, res).thenRun(() -> res.complete(null));
        return res;
    }


    private static CompletableFuture<Void> findAnyBaseType(@Nullable Type type, Function<? super Type, ? extends CompletableFuture<Boolean>> checker, CompletableFuture<Type> res) {
        if (type == null || res.isDone()) {
            return CompletableFuture.completedFuture(null);
        }


        CompletableFuture<Void> self = ((CompletableFuture) checker.apply(type)).thenAccept(r -> {

            // todo: check
            if (r.equals(true)) {
                res.complete(type);
            }
        });


        new Exception().printStackTrace();
        if (type instanceof ClassType) {
//            return CompletableFuture.allOf(self,
//
//                    superclass((ClassType) type).thenCompose(s -> findAnyBaseType(s, checker, res)),
//                    interfaces((ClassType) type)
//                            .thenCompose(interfaces ->
//                                    CompletableFuture.allOf(interfaces.stream().map(()).toArray(()))));
        }


        if (type instanceof InterfaceType) {
//            return CompletableFuture.allOf(self,
//
//                    superinterfaces((InterfaceType) type)
//                            .thenCompose(interfaces -> CompletableFuture.allOf(interfaces.stream().map(()).toArray(()))));
        }


        return self;
    }


    public static CompletableFuture<List<Method>> methods(ReferenceType type) {
        return CompletableFuture.completedFuture(type.methods());
    }

    public static CompletableFuture<List<InterfaceType>> superinterfaces(InterfaceType iface) {
        return CompletableFuture.completedFuture(iface.superinterfaces());
    }

    public static CompletableFuture<ClassType> superclass(ClassType cls) {
        return CompletableFuture.completedFuture(cls.superclass());
    }

    public static CompletableFuture<List<InterfaceType>> interfaces(ClassType cls) {
        return CompletableFuture.completedFuture(cls.interfaces());
    }


//    public static CompletableFuture<Stream<? extends ReferenceType>> supertypes(ReferenceType type) {
//        if (type instanceof InterfaceType)
//            return superinterfaces((InterfaceType) type).thenApply(Collection::stream);
//        if (type instanceof ClassType) {
//            return superclass((ClassType) type)
//                    .thenCombine(
//                            interfaces((ClassType) type), (superclass, interfaces) -> StreamEx.ofNullable(superclass).prepend(interfaces));
//        }
//
//
//        return CompletableFuture.completedFuture(StreamEx.empty());
//    }

    public static Throwable unwrap(Throwable throwable) {
        return (throwable instanceof java.util.concurrent.CompletionException) ? throwable.getCause() : throwable;
    }

    private static <T> CompletableFuture<T> async(Computable<? extends T> provider) {
        return CompletableFuture.completedFuture(null).thenApply(__ -> provider.compute());
    }

    private static <T> void completeFuture(T res, Throwable ex, CompletableFuture<T> future) {
        if (ex != null) {
            future.completeExceptionally(ex);
        } else {
            future.complete(res);
        }
    }
}
