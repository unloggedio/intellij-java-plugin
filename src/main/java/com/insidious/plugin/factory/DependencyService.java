package com.insidious.plugin.factory;

import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.pojo.ModuleInformation;
import com.insidious.plugin.pojo.ProjectTypeInfo;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.dom.MavenDomUtil;
import org.jetbrains.idea.maven.dom.model.MavenDomDependency;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DependencyService {
    private static final Logger logger = LoggerUtil.getInstance(DependencyService.class);

    @NotNull
    private static String getMavenArtifactKey(MavenId mavenId) {
        StringBuilder builder = new StringBuilder();
        append(builder, mavenId.getGroupId());
        append(builder, mavenId.getArtifactId());
        append(builder, mavenId.getVersion());

        return builder.toString();
    }

    private static void append(StringBuilder builder, String part) {
        if (builder.length() != 0) builder.append(':');
        builder.append(part == null ? "" : part);
    }

    public void addDependency(
            Project project,
            final Collection<String> dependencies,
            final ModuleInformation moduleInformation,
            final InsidiousService insidiousService) {

        @NotNull LibraryTable librariesTable = LibraryTablesRegistrar.getInstance()
                .getLibraryTable(project);
        for (Library library : librariesTable.getLibraries()) {
            if (library.getName().startsWith("Gradle:")) {
                if (library.getName().contains("jackson-core")) {
                    String[] parts = library.getName()
                            .split(":");
                    insidiousService.getProjectTypeInfo()
                            .setJacksonDatabindVersion(parts[parts.length - 1]);
                }
            }
        }


        List<MavenId> ids = dependencies.stream()
                .map(e -> getMavenDependency(e, insidiousService.getProjectTypeInfo()
                        .getJacksonDatabindVersion()))
                .collect(
                        Collectors.toList());

        if (insidiousService.findBuildSystemForModule(moduleInformation.getName())
                .equals(InsidiousService.PROJECT_BUILD_SYSTEM.MAVEN)) {
            MavenProjectsManager manager = MavenProjectsManager.getInstance(project);
            @NotNull List<MavenProject> mavenProjects = manager.getProjects();
            MavenProject mavenProject = null;
            for (MavenProject mavenProject1 : mavenProjects) {
                if (Objects.requireNonNull(mavenProject1.getName())
                        .equalsIgnoreCase(moduleInformation.getName())) {
                    mavenProject = mavenProject1;
                    break;
                }
            }
            if (mavenProject == null) {
                logger.warn("Did not find maven project by name: " + moduleInformation);
                return;
            }


            final MavenDomProjectModel model = MavenDomUtil.getMavenDomProjectModel(project, mavenProject.getFile());
            if (model == null) return;

            WriteCommandAction.writeCommandAction(project, DomUtil.getFile(model))
                    .withName("Add Maven dependency")
                    .run(() -> {

                        Map<MavenId, MavenDomDependency> dependencyMap = model.getDependencies()
                                .getDependencies()
                                .stream()
                                .collect(
                                        Collectors.toMap(
                                                it -> new MavenId(it.getGroupId()
                                                        .getStringValue(), it.getArtifactId()
                                                        .getStringValue(), it.getVersion()
                                                        .getStringValue()),
                                                Function.identity()
                                        ));


                        for (MavenId each : ids) {
                            MavenDomDependency existingDependency = dependencyMap.get(each);
                            if (existingDependency == null) {
                                MavenDomDependency dependency = MavenDomUtil.createDomDependency(model, null, each);
                            } else {
                                if ("test".equals(existingDependency.getScope()
                                        .getStringValue())) {
                                    existingDependency.getScope()
                                            .setValue(null);
                                }
                            }
                        }
                    });

            FileDocumentManager.getInstance()
                    .saveAllDocuments();
            MavenProjectsManager.getInstance(project)
                    .forceUpdateAllProjectsOrFindAllAvailablePomFiles();
        } else {

            VirtualFile gradleFile =
                    LocalFileSystem.getInstance().findFileByIoFile(new File(moduleInformation.getPath() + File.separator +
                    "build.gradle"));
            if (gradleFile == null) {
                InsidiousNotification.notifyMessage("Failed to locate build.gradle in " + moduleInformation.getPath(),
                        NotificationType.ERROR);
            }
            PsiFile file = PsiUtilBase.getPsiFile(project, gradleFile);

            WriteCommandAction.writeCommandAction(project, file)
                    .withName("Add Maven dependency")
                    .run(() -> {
                        GroovyPsiElementFactory factory = GroovyPsiElementFactory.getInstance(project);
                        List<GrMethodCall> closableBlocks = PsiTreeUtil.getChildrenOfTypeAsList(file,
                                GrMethodCall.class);
                        GrCall dependenciesBlock = ContainerUtil.find(closableBlocks, call -> {
                            GrExpression expression = call.getInvokedExpression();
                            return "dependencies".equals(expression.getText());
                        });

                        if (dependenciesBlock == null) {
                            StringBuilder buf = new StringBuilder();
                            for (MavenId mavenId : ids) {
                                buf.append(String.format("implementation '%s'\n", getMavenArtifactKey(mavenId)));
                            }
                            dependenciesBlock = (GrCall) factory.createStatementFromText("dependencies{\n" + buf + "}");
                            file.add(dependenciesBlock);
                        } else {
                            GrClosableBlock closableBlock = ArrayUtil.getFirstElement(
                                    dependenciesBlock.getClosureArguments());
                            if (closableBlock != null) {
                                for (MavenId mavenId : ids) {
                                    closableBlock.addStatementBefore(
                                            factory.createStatementFromText(
                                                    String.format("compile '%s'\n", getMavenArtifactKey(mavenId))),
                                            null);
                                }
                            }
                        }
                    });

        }

    }

    public MavenId getMavenDependency(String dependency, String version) {

        String groupId;
        switch (dependency) {
            case "gson":
                groupId = "com.google.code.gson";
                version = "2.10";
                break;
            case "jackson":
            default:
                groupId = "com.fasterxml.jackson.datatype";
                break;
        }

        return new MavenId(groupId, dependency, version);

    }


}
