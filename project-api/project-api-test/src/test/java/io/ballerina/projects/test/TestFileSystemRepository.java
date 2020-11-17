package io.ballerina.projects.test;

import io.ballerina.projects.Package;
import io.ballerina.projects.PackageName;
import io.ballerina.projects.PackageOrg;
import io.ballerina.projects.SemanticVersion;
import io.ballerina.projects.environment.PackageLoadRequest;
import io.ballerina.projects.environment.PackageRepository;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Test file system repository.
 *
 * @since 2.0.0
 */
public class TestFileSystemRepository {
    private static final Path MOCK_REPO = Paths.get("src/test/resources/mock-repo");

    Path mockRepo;
    PackageRepository repo;

    PackageLoadRequest aPkg = new PackageLoadRequest(PackageOrg.BALLERINA_ORG,
            PackageName.from("lang.annotations"),
            SemanticVersion.from("1.0.0"));
    PackageLoadRequest bPkg = new PackageLoadRequest(PackageOrg.from("wso2"),
            PackageName.from("twitter"),
            SemanticVersion.from("1.0.0"));

//    @BeforeSuite
    void setUpMockRepo() throws IOException {
        mockRepo = Files.createTempDirectory("fsrepo-");
        // move mock repo to tmp
        FileUtils.copyDirectory(MOCK_REPO.toFile(), mockRepo.toFile());
//        repo = new FileSystemRepository(mockRepo);
    }

//    @Test
    public void testGetPackage() {
        // Test if it returns an existing package
        Optional<Package> aOptionalPackage = repo.getPackage(aPkg);
        Assert.assertFalse(aOptionalPackage.isEmpty());
        if (!aOptionalPackage.isEmpty()) {
            Package aPackage = aOptionalPackage.get();
            Assert.assertEquals(aPackage.packageOrg().value(), "ballerina");
            Assert.assertEquals(aPackage.packageName().value(), "lang.annotations");
            Assert.assertEquals(aPackage.packageVersion().value(), SemanticVersion.from("1.0.0"));
        }

        // Test if it returns for an empty package
        Optional<Package> bPackage = repo.getPackage(bPkg);
        Assert.assertTrue(bPackage.isEmpty());
    }

//    @Test
    public void testGetPackageVersions() {
        // Package that has versions
        List<SemanticVersion> aPackageVersions = repo.getPackageVersions(aPkg);
        Assert.assertEquals(aPackageVersions.size(), 2);
        // Invalid package
        List<SemanticVersion> bPackageVersions = repo.getPackageVersions(bPkg);
        Assert.assertEquals(bPackageVersions.size(), 0);
    }

}
