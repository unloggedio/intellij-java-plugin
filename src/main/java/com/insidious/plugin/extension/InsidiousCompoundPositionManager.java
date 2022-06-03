package com.insidious.plugin.extension;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.MultiRequestPositionManager;
import com.intellij.debugger.NoDataException;
import com.intellij.debugger.PositionManager;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.impl.DebuggerUtilsImpl;
import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.intellij.execution.filters.LineNumbersMapping;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.ClassPrepareRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class InsidiousCompoundPositionManager implements MultiRequestPositionManager {
    public static final InsidiousCompoundPositionManager EMPTY = new InsidiousCompoundPositionManager();
    private static final Logger logger = LoggerUtil.getInstance(InsidiousCompoundPositionManager.class);
    private final ArrayList<PositionManager> myPositionManagers = new ArrayList<>();


    private final Map<Location, SourcePosition> mySourcePositionCache;


    public InsidiousCompoundPositionManager() {
        this.mySourcePositionCache = new WeakHashMap<>();
    }


    public InsidiousCompoundPositionManager(PositionManager manager) {
        this.mySourcePositionCache = new WeakHashMap<>();
        appendPositionManager(manager);
    }

    private static boolean checkCacheEntry(@Nullable SourcePosition position, @NotNull Location location) {
        if (position == null) return false;
        PsiFile psiFile = position.getFile();
        if (!psiFile.isValid()) return false;

        String url = DebuggerUtilsEx.getAlternativeSourceUrl(location
                .declaringType().name(), psiFile.getProject());
        if (url == null) return true;
        VirtualFile file = psiFile.getVirtualFile();
        return (file != null && url.equals(file.getUrl()));
    }

    public void appendPositionManager(PositionManager manager) {
        this.myPositionManagers.remove(manager);
        this.myPositionManagers.add(0, manager);
        clearCache();
    }

    public void clearCache() {
        this.mySourcePositionCache.clear();
    }

    private <T> T iterate(Producer<? extends T> processor, T defaultValue, SourcePosition position) {
        return iterate(processor, defaultValue, position, true);
    }

    private <T> T iterate(Producer<? extends T> processor, T defaultValue, SourcePosition position, boolean ignorePCE) {
        FileType fileType = (position != null) ? position.getFile().getFileType() : null;
        for (PositionManager positionManager : this.myPositionManagers) {
            if (fileType != null) {
                Set<? extends FileType> types = positionManager.getAcceptedFileTypes();
                if (types != null && !types.contains(fileType)) {
                    continue;
                }
            }
            try {
                if (!ignorePCE) {
                    ProgressManager.checkCanceled();
                }
                return DebuggerUtilsImpl.suppressExceptions(() -> processor.produce(positionManager), defaultValue, ignorePCE, NoDataException.class);


            } catch (NoDataException noDataException) {
            }
        }

        return defaultValue;
    }

    @Nullable
    public SourcePosition getSourcePosition(Location location) {
        if (location == null) return null;
        return ReadAction.compute(() -> {
            SourcePosition res = null;

            try {
                res = this.mySourcePositionCache.get(location);
            } catch (IllegalArgumentException illegalArgumentException) {
            }
            return res;
//            return checkCacheEntry(res, location) ? res : iterate((), null, null, false);
        });
    }

    @NotNull
    public List<ReferenceType> getAllClasses(@NotNull SourcePosition classPosition) {
        return iterate(positionManager -> positionManager.getAllClasses(classPosition), Collections.emptyList(), classPosition);
    }


    @NotNull
    public List<Location> locationsOfLine(@NotNull ReferenceType type, @NotNull SourcePosition position) {
        VirtualFile file = position.getFile().getVirtualFile();
        if (file != null) {

            LineNumbersMapping mapping = file.getUserData(LineNumbersMapping.LINE_NUMBERS_MAPPING_KEY);
            if (mapping != null) {
                int line = mapping.sourceToBytecode(position.getLine() + 1);
                if (line > -1) {
                    position = SourcePosition.createFromLine(position.getFile(), line - 1);
                }
            }
        }

        SourcePosition finalPosition = position;
        return iterate(positionManager -> positionManager.locationsOfLine(type, finalPosition), Collections.emptyList(), position);
    }


    public ClassPrepareRequest createPrepareRequest(@NotNull ClassPrepareRequestor requestor, @NotNull SourcePosition position) {
        return iterate(positionManager -> positionManager.createPrepareRequest(requestor, position), null, position);
    }


    @NotNull
    public List<ClassPrepareRequest> createPrepareRequests(@NotNull ClassPrepareRequestor requestor, @NotNull SourcePosition position) {
        return iterate(positionManager -> {
            if (positionManager instanceof MultiRequestPositionManager)
                return ((MultiRequestPositionManager) positionManager).createPrepareRequests(requestor, position);
            ClassPrepareRequest prepareRequest = positionManager.createPrepareRequest(requestor, position);
            return (prepareRequest == null) ? Collections.emptyList() : Collections.singletonList(prepareRequest);
        }, Collections.emptyList(), position);
    }

    private interface Producer<T> {
        T produce(PositionManager param1PositionManager) throws NoDataException;
    }
}


