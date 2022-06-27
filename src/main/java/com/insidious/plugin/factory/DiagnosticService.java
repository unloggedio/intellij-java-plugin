package com.insidious.plugin.factory;

import com.insidious.plugin.Constants;
import com.insidious.plugin.client.MultipartUtility;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.extension.InsidiousNotification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.insidious.plugin.factory.InsidiousService.HOSTNAME;

public class DiagnosticService {
    private final VersionManager versionManager;
    private final Project project;
    private final Module module;

    public DiagnosticService(VersionManager versionManager,
                             Project project, Module module) {
        this.versionManager = versionManager;
        this.project = project;
        this.module = module;
    }

    public void generateAndUploadReport() {


        VideobugLocalClient localClient = new VideobugLocalClient(Objects.requireNonNull(project.getBasePath()));
        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append("hostname: ").append(HOSTNAME).append("\n");
        reportBuilder.append("version: ").append(versionManager.getVersion()).append("\n");
        reportBuilder.append("branch: ").append(versionManager.getGitBranchName()).append("\n");
        reportBuilder.append("hash").append(versionManager.getGitHash()).append("\n");
        reportBuilder.append("last tag").append(versionManager.getGitLastTag()).append("\n");


        List<ExecutionSession> identifiedSessions = localClient.fetchProjectSessions().getItems();


        ProjectManager projectManager = ProjectManager.getInstance();

        Project[] openProjects = projectManager.getOpenProjects();
        reportBuilder.append("listing open projects").append("\n");
        for (Project openProject : openProjects) {
            reportBuilder.append("project: ").append(openProject.getName())
                    .append(" from location ").append(openProject.getBasePath()).append("\n");
        }
        reportBuilder.append("default project: ").append(projectManager.getDefaultProject().getName()).append("\n");


        File agentJarFile = Constants.VIDEOBUG_AGENT_PATH.toFile();
        if (agentJarFile.exists()) {
            reportBuilder.append("agent jar exists at: ").append(agentJarFile.getAbsolutePath()).append("\n");
            reportBuilder.append("agent jar downloaded at: ").append(agentJarFile.lastModified()).append("\n");
            reportBuilder.append("agent jar size: ").append(agentJarFile.length()).append("\n");
        } else {
            reportBuilder.append("agent jar DOES NOT exists at: ").append(agentJarFile.getAbsolutePath()).append("\n");
        }

        Project selectedProject = this.project;
        reportBuilder.append("projected selected for videobug: ").append(selectedProject.getName()).append("\n");
        ModuleManager moduleManager = ModuleManager.getInstance(selectedProject);

        reportBuilder.append("listing modules in default project").append("\n");
        for (Module module : moduleManager.getModules()) {
            ModuleType<?> moduleType = ModuleType.get(module);
            reportBuilder.append("module name: ").append(module.getName())
                    .append(" => ").append(moduleType.getName()).append("\n");
        }

        for (ExecutionSession identifiedSession : identifiedSessions) {
            reportBuilder.append("session identified by vlc: ").append(identifiedSession.getSessionId()).append("\n");
        }


        File projectRootPath = new File(Objects.requireNonNull(selectedProject.getBasePath()));
        if (!projectRootPath.exists()) {
            reportBuilder.append("project root path doesnt exist ?").append("\n");
        } else {
            String[] folderContentList = projectRootPath.list();
            if (folderContentList == null) {
                reportBuilder.append("file list in current folder is empty").append("\n");
            } else {
                for (String fileInRootPath : folderContentList) {
                    if (fileInRootPath.startsWith("selogger")) {
                        Path logFolder = Path.of(selectedProject.getBasePath(), fileInRootPath);
                        logFolderToReport(reportBuilder, logFolder.toFile());
                    }
                }
            }
        }

        String reportContent = reportBuilder.toString();
        ByteArrayOutputStream compressedReportStream = new ByteArrayOutputStream();
        ZipOutputStream zipReport = new ZipOutputStream(compressedReportStream);
        byte[] compressedReportContents = new byte[0];
        ZipEntry zipEntry = new ZipEntry("report.txt");
        try {
            zipReport.putNextEntry(zipEntry);
            zipReport.write(reportContent.getBytes(StandardCharsets.UTF_8));
            zipReport.closeEntry();
            zipReport.finish();
            compressedReportContents = compressedReportStream.toByteArray();
        } catch (IOException e) {
            InsidiousNotification.notifyMessage("unable to compress report: "
                    + e.getMessage(), NotificationType.ERROR);
            try (FileOutputStream plainTextReport = new FileOutputStream(
                    projectRootPath.getAbsoluteFile() + "/videobug-report.txt")) {
                plainTextReport.write(reportContent.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ex) {
                InsidiousNotification.notifyMessage("Unable to write report on disk: "
                        + ex.getMessage(), NotificationType.ERROR);

            }
            return;
        }

//        if (identifiedSessions.size() > 0) {
//            ExecutionSession latestSession = identifiedSessions.get(0);
//            localClient.setSession(latestSession);
//            reportBuilder.append("adding logs for latest session: ").append(latestSession.getSessionId()).append("\n");
//            List<File> sessionFiles = localClient.getSessionFiles();
//            reportBuilder.append("identified session files by vlc: ").append(sessionFiles.toString()).append("\n");
//
//            localClient.getTracesByObjectValue();
//
//        }

        File file = new File(projectRootPath.getAbsoluteFile() + "/videobug-report.zip");
        if (file.exists()) {
            file.delete();
        }
        try (FileOutputStream localOutput = new FileOutputStream(file)) {
            localOutput.write(compressedReportContents);
        } catch (IOException e) {
            InsidiousNotification.notifyMessage(
                    "failed to save report locally. " +
                            "Error was: " + e.getMessage(),
                    NotificationType.ERROR
            );
        }

        try {

            sendPOSTRequest(
                    "https://cloud.bug.video/checkpoint/uploadReport",
                    new ByteArrayInputStream(compressedReportContents)
            );

//            sendPOSTRequest(
//                    "http://localhost:8123/checkpoint/uploadReport",
//                    new ByteArrayInputStream(compressedReportContents)
//            );

        } catch (IOException ex) {
            InsidiousNotification.notifyMessage(
                    "failed to upload report to server, saving on disk as videobug-report.zip. " +
                            "Error was: " + ex.getMessage(),
                    NotificationType.ERROR
            );
        }

        InsidiousNotification.notifyMessage(
                "Report generated and uploaded: videobug-report.zip",
                NotificationType.INFORMATION
        );
    }


    private void logFolderToReport(StringBuilder reportBuilder, File logFolder) {
        reportBuilder.append("log folder: ").append(logFolder.getName()).append("\n");
        String fileInRootPath = logFolder.getName();

        String[] loggedFiles = logFolder.list();
        if (loggedFiles == null) {
            reportBuilder.append("no files found in [").append(fileInRootPath).append("]").append("\n");
            return;
        } else {
            for (String loggedFile : loggedFiles) {
                File loggedFileInstance = Path.of(logFolder.getAbsolutePath(), loggedFile).toFile();
                reportBuilder.append("file in [").append(fileInRootPath).append("]: ")
                        .append(loggedFile).append(" == Size ==> ").append(loggedFileInstance.length()).append("\n");
                if (loggedFile.endsWith(".zip")) {
                    logZipToReport(reportBuilder, loggedFileInstance);
                }
            }
        }


        Path logFilePath = Path.of(logFolder.getAbsolutePath(), fileInRootPath, "log.txt");
        File logFile = logFilePath.toFile();
        if (!logFile.exists()) {
            reportBuilder.append("log.txt does not exist in [").append(fileInRootPath).append("]").append("\n");
        } else {
            FileInputStream logFileInputStream = null;
            try {
                logFileInputStream = new FileInputStream(logFile);
                String logFileContents = streamToString(logFileInputStream);
                reportBuilder.append("===== BEGIN  LOG.TXT ======").append("\n");
                reportBuilder.append(logFileContents).append("\n");
                reportBuilder.append("===== END OF LOG.TXT ======").append("\n");
            } catch (IOException ex) {
                reportBuilder.append("failed to read log file: ").append(ex.getMessage()).append("\n");
            }
        }
    }

    private void logZipToReport(StringBuilder reportBuilder, File loggedFileInstance) {
        try {
            ZipInputStream zipReader = new ZipInputStream(new FileInputStream(loggedFileInstance));
            ZipEntry zipEntry;
            while (true) {
                try {
                    zipEntry = zipReader.getNextEntry();
                    if (zipEntry == null) {
                        break;
                    }
                    reportBuilder.append("zip [").append(loggedFileInstance.getName())
                            .append("] content => [").append(zipEntry.getName())
                            .append("] size => [").append(zipEntry.getSize())
                            .append("]").append("\n");
                } catch (IOException e) {
                    reportBuilder.append("failed to open entry in zip [")
                            .append(loggedFileInstance.getName()).append("] => ")
                            .append(e.getMessage()).append("\n");
                }

            }
        } catch (FileNotFoundException e) {
            reportBuilder.append("failed to open logzip [")
                    .append(loggedFileInstance.getName()).append("] => ").append(e.getMessage()).append("\n");
        }
    }

    private void sendPOSTRequest(String url, ByteArrayInputStream byteArrayInputStream) throws IOException {
        String charset = "UTF-8";
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "unlogged-plugin/" + versionManager.getVersion());

        MultipartUtility form = new MultipartUtility(url, charset, headers);

        form.addStream("file", "diagnostic-report-" + System.currentTimeMillis() + ".zip",
                byteArrayInputStream);
        form.addFormField("hostname", HOSTNAME);
        form.addFormField("projectId", module.getName());
        form.addFormField("plugin-version", versionManager.getVersion());
        String response = form.finish();

    }

    private String streamToString(InputStream stream) throws IOException {
        int bufferSize = 1024;
        char[] buffer = new char[bufferSize];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(stream, StandardCharsets.UTF_8);
        for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
            out.append(buffer, 0, numRead);
        }
        return out.toString();
    }

}
