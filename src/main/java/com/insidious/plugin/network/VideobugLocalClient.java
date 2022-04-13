package com.insidious.plugin.network;

import com.insidious.plugin.callbacks.*;
import com.insidious.plugin.extension.connector.model.ProjectItem;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.index.InsidiousIndexParser;
import com.insidious.plugin.network.pojo.DataResponse;
import com.insidious.plugin.network.pojo.ExecutionSession;
import com.insidious.plugin.network.pojo.FilteredDataEventsRequest;
import com.insidious.plugin.network.pojo.SigninRequest;
import com.insidious.plugin.network.pojo.exceptions.UnauthorizedException;
import com.insidious.plugin.util.LoggerUtil;
import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.KaitaiStream;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class VideobugLocalClient implements VideobugClientInterface {
    private static final Logger logger = LoggerUtil.getInstance(VideobugLocalClient.class);
    private final String pathToSessions;
    private ExecutionSession session;
    private ProjectItem currentProject;

    public VideobugLocalClient(String pathToSessions) {
        this.pathToSessions = pathToSessions;
    }

    @Override
    public ExecutionSession getCurrentSession() {
        return this.session;
    }

    @Override
    public void setSession(ExecutionSession session) {
        this.session = session;
    }

    @Override
    public void signup(String serverUrl, String username, String password, SignUpCallback callback) {
        callback.success();
    }

    @Override
    public void signin(SigninRequest signinRequest, SignInCallback signInCallback) throws UnauthorizedException {
        signInCallback.success("localhost");
    }

    @Override
    public void getProjectByName(String projectName, GetProjectCallback getProjectCallback) {
        getProjectCallback.success(projectName);
    }

    @Override
    public ProjectItem fetchProjectByName(String projectName) {
        ProjectItem projectItem = new ProjectItem();
        projectItem.setName(projectName);
        projectItem.setId("1");
        projectItem.setCreatedAt(new Date().toString());
        return projectItem;
    }

    @Override
    public void createProject(String projectName, NewProjectCallback newProjectCallback) {
        newProjectCallback.success("1");
    }

    @Override
    public void getProjectToken(ProjectTokenCallback projectTokenCallback) {
        projectTokenCallback.success("localhost-token");
    }

    @Override
    public void getProjectSessions(GetProjectSessionsCallback getProjectSessionsCallback) throws IOException {
        getProjectSessionsCallback.success(getLocalSessions());
    }

    private List<ExecutionSession> getLocalSessions() {
        List<ExecutionSession> list = new LinkedList<>();
        File currentDir = new File(pathToSessions);
        logger.info("looking for sessions for project in [{}]", currentDir.getAbsolutePath());
        for (File file : Objects.requireNonNull(currentDir.listFiles())) {
            if (file.isDirectory() && file.getName().contains("selogger")) {
                ExecutionSession executionSession = new ExecutionSession();
                executionSession.setId(file.getName());
                executionSession.setCreatedAt(new Date(file.lastModified()));
                executionSession.setHostname("localhost");
                executionSession.setLastUpdateAt(file.lastModified());
                executionSession.setName(file.getName());
                list.add(executionSession);
            }
        }
        return list;

    }

    @Override
    public DataResponse<ExecutionSession> fetchProjectSessions() {
        List<ExecutionSession> localSessions = getLocalSessions();
        return new DataResponse<>(localSessions, localSessions.size(), 1);
    }

    @Override
    public void getTracesByObjectType(
            List<String> classList,
            GetProjectSessionErrorsCallback getProjectSessionErrorsCallback) throws IOException {
        logger.info("trace by class");
        File sessionFolder = new File(this.pathToSessions + "\\" + session.getName());

        List<File> indexFiles = new LinkedList<>();
        for (File file : sessionFolder.listFiles()) {
            if (file.getName().startsWith("index") && file.getName().endsWith("zip")) {
                indexFiles.add(file);
            }
        }

        for (File indexFile : indexFiles) {
            ZipInputStream zippedIndexFile = new ZipInputStream(new FileInputStream(indexFile));
            ZipEntry nextEntry = zippedIndexFile.getNextEntry();
            assert nextEntry != null;
            if (nextEntry.getName().equals("bug.video.index")) {
                byte[] indexBytes = zippedIndexFile.readAllBytes();
                InsidiousIndexParser archiveIndexContents
                        = new InsidiousIndexParser(new ByteBufferKaitaiStream(indexBytes));

                logger.info("[{}] indexed files in the archive", archiveIndexContents.indexFileCount());
//                archiveIndexContents.
            }
        }


    }

    @Override
    public void getTracesByObjectValue(
            String traceId,
            GetProjectSessionErrorsCallback getProjectSessionErrorsCallback) {
        logger.info("trace by string value");
    }

    @Override
    public ReplayData fetchDataEvents(
            FilteredDataEventsRequest filteredDataEventsRequest) {
        return null;
    }

    @Override
    public String getToken() {
        return "localhost-token";
    }

    @Override
    public ProjectItem getProject() {
        return this.currentProject;
    }

    @Override
    public void setProject(String projectName) {
        ProjectItem currentProject = new ProjectItem();
        currentProject.setName(projectName);
        currentProject.setId("1");
        currentProject.setCreatedAt(new Date().toString());
        this.currentProject = currentProject;
    }

    @Override
    public String getEndpoint() {
        return pathToSessions;
    }

    @Override
    public void getAgentDownloadUrl(AgentDownloadUrlCallback agentDownloadUrlCallback) {
        agentDownloadUrlCallback.success("");
    }

    @Override
    public void downloadAgentFromUrl(String url, String insidiousLocalPath, boolean overwrite) {

    }
}
