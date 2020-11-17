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

package io.ballerina.projects.internal.environment;

import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageId;
import io.ballerina.projects.Project;
import io.ballerina.projects.SemanticVersion;
import io.ballerina.projects.environment.GlobalPackageCache;
import io.ballerina.projects.environment.ModuleLoadRequest;
import io.ballerina.projects.environment.ModuleLoadResponse;
import io.ballerina.projects.environment.PackageLoadRequest;
import io.ballerina.projects.environment.PackageRepository;
import io.ballerina.projects.environment.PackageResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Default Package resolver for Ballerina project.
 *
 * @since 2.0.0
 */
public class DefaultPackageResolver extends PackageResolver {
    private final Project project;
    private final PackageRepository distRepository;
    private final GlobalPackageCache globalPackageCache;

    public DefaultPackageResolver(Project project, PackageRepository distCache, GlobalPackageCache globalPackageCache) {
        this.project = project;
        this.distRepository = distCache;
        this.globalPackageCache = globalPackageCache;
    }

    public Collection<ModuleLoadResponse> loadPackages(Collection<ModuleLoadRequest> moduleLoadRequests) {
        // TODO This is a dummy implementation that checks only the current package in the project.
        List<ModuleLoadResponse> modLoadResponses = new ArrayList<>();
        Package currentPkg = this.project.currentPackage();
        for (ModuleLoadRequest modLoadRequest : moduleLoadRequests) {
            Module module = null;
            if (currentPkg.packageName().equals(modLoadRequest.packageName())) {
                module = currentPkg.module(modLoadRequest.moduleName());
            }

            if (module == null) {
                // Try to get from the already loaded packages
                module = loadFromCache(modLoadRequest);
            }

            if (module == null) {
                module = loadFromDistributionCache(modLoadRequest);
            }

            if (module == null) {
                continue;
            }
            modLoadResponses.add(new ModuleLoadResponse(module.packageInstance().packageId(), module.moduleId(),
                    modLoadRequest));
        }
        return modLoadResponses;
    }

    public Package getPackage(PackageId packageId) {
        if (project.currentPackage().packageId() == packageId) {
            return project.currentPackage();
        }
        // TODO Improve this logic
        return globalPackageCache.getPackage(packageId).orElse(null);
    }

    private Module loadFromDistributionCache(ModuleLoadRequest modLoadRequest) {
        // If version is null load the latest package
        PackageLoadRequest loadRequest = PackageLoadRequest.from(modLoadRequest);
        if (loadRequest.version().isEmpty()) {
            // find the latest version
            List<SemanticVersion> packageVersions = distRepository.getPackageVersions(loadRequest);
            if (packageVersions.isEmpty()) {
                // no versions found.
                // todo handle package not found with exception
                return null;
            }
            SemanticVersion latest = findlatest(packageVersions);
            loadRequest = new PackageLoadRequest(loadRequest.orgName().orElse(null), loadRequest.packageName(), latest);
        }

        Optional<Package> packageOptional = distRepository.getPackage(loadRequest);
        return packageOptional.map(pkg -> {
            pkg.resolveDependencies();
            globalPackageCache.cache(pkg);
            // todo fetch the correct module and return
            return pkg.getDefaultModule();
        }).orElse(null);
    }

    private SemanticVersion findlatest(List<SemanticVersion> packageVersions) {
        // todo Fix me
        return packageVersions.get(0);
    }

    private Module loadFromCache(ModuleLoadRequest modLoadRequest) {
        // TODO improve the logic
        List<Package> packageList = globalPackageCache.getPackages(
                modLoadRequest.orgName().orElse(null), modLoadRequest.packageName());

        if (packageList.isEmpty()) {
            return null;
        }

        Package pkg = packageList.get(0);
        return pkg.module(modLoadRequest.moduleName());
    }
}
