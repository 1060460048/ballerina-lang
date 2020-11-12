/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerina.projects.directory;

import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.PackageConfig;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectEnvironmentBuilder;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.model.BallerinaToml;
import io.ballerina.projects.util.ProjectConstants;
import io.ballerina.projects.util.ProjectUtils;

import java.nio.file.Path;
import java.util.Optional;

/**
 * {@code BuildProject} represents Ballerina project instance created from the project directory.
 *
 * @since 2.0.0
 */
public class BuildProject extends Project {

    /**
     * Loads a BuildProject from the provided path.
     *
     * @param projectPath Ballerina project path
     * @return build project
     */
    public static BuildProject load(ProjectEnvironmentBuilder environmentBuilder, Path projectPath) {
        Path absProjectPath = Optional.of(projectPath.toAbsolutePath()).get();
        if (!absProjectPath.toFile().exists()) {
            throw new RuntimeException("project path does not exist:" + projectPath);
        }

        if (!ProjectUtils.isBallerinaProject(absProjectPath)) {
            throw new RuntimeException("provided path is not a valid Ballerina project: " + projectPath);
        }

        if (ProjectUtils.findProjectRoot(Optional.of(absProjectPath.getParent()).get()) != null) {
            throw new RuntimeException("provided path is already within a Ballerina project: " +
                    absProjectPath.getParent());
        }

        return new BuildProject(environmentBuilder, absProjectPath);
    }

    /**
     * Loads a BuildProject from the provided path.
     *
     * @param projectPath Ballerina project path
     * @return build project
     */
    public static BuildProject load(Path projectPath) {
        return load(ProjectEnvironmentBuilder.getDefaultBuilder(), projectPath);
    }

    private BuildProject(ProjectEnvironmentBuilder environmentBuilder, Path projectPath) {
        super(ProjectKind.BUILD_PROJECT, projectPath, environmentBuilder);

        // load Ballerina.toml
        Path ballerinaTomlPath = this.sourceRoot.resolve(ProjectConstants.BALLERINA_TOML);
        BallerinaToml ballerinaToml = BallerinaTomlProcessor.parse(ballerinaTomlPath);

        // Set default build options
        if (ballerinaToml.getBuildOptions() != null) {
            this.setBuildOptions(ballerinaToml.getBuildOptions());
        } else {
            this.setBuildOptions(new BuildOptions());
        }

        addPackage(projectPath);
    }

    public BuildOptions getBuildOptions() {
        return (BuildOptions) super.getBuildOptions();
    }

    public Optional<Path> modulePath(ModuleId moduleId) {
        if (currentPackage().moduleIds().contains(moduleId)) {
            if (currentPackage().getDefaultModule().moduleId() == moduleId) {
                return Optional.of(sourceRoot);
            } else {
                return Optional.of(sourceRoot.resolve(ProjectConstants.MODULES_ROOT).resolve(
                        currentPackage().module(moduleId).moduleName().moduleNamePart()));
            }
        }
        return Optional.empty();
    }

    public Optional<Path> documentPath(DocumentId documentId) {
        for (ModuleId moduleId : currentPackage().moduleIds()) {
            Module module = currentPackage().module(moduleId);
            Optional<Path> modulePath = modulePath(moduleId);
            if (module.documentIds().contains(documentId)) {
                if (modulePath.isPresent()) {
                    return Optional.of(modulePath.get().resolve(module.document(documentId).name()));
                }
            } else if (module.testDocumentIds().contains(documentId)) {
                if (modulePath.isPresent()) {
                    return Optional.of(modulePath.get()
                            .resolve(ProjectConstants.TEST_DIR_NAME).resolve(module.document(documentId).name()));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Loads a package in the provided project path.
     *
     * @param projectPath project path
     */
    private void addPackage(Path projectPath) {
        PackageDescriptor packageDescriptor = ProjectFiles.createPackageDescriptor(
                projectPath.resolve(ProjectConstants.BALLERINA_TOML));
        final PackageConfig packageConfig = PackageLoader.loadPackage(projectPath, false, packageDescriptor);
        this.addPackage(packageConfig);
    }

    /**
     * {@code BuildOptions} represents build options specific to a build project.
     */
    public static class BuildOptions extends io.ballerina.projects.BuildOptions {

        private BuildOptions() {}

        public void setObservabilityEnabled(boolean observabilityEnabled) {
            this.observabilityIncluded = observabilityEnabled;
        }

        public void setSkipLock(boolean skipLock) {
            this.skipLock = skipLock;
        }

        public void setCodeCoverage(boolean codeCoverage) {
            this.codeCoverage = codeCoverage;
        }
    }
}
