package pw.krejci.mrc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.utils.io.FileUtils;

/**
 * @author Lukas Krejci
 * @since 0.1.0
 */
@Mojo( name = "jar", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.RUNTIME )
public class JarMojo extends org.apache.maven.plugins.jar.JarMojo {

    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true, required = true)
    private File buildOutputDirectory;

    @Parameter(defaultValue = "${project.build.directory}/multi-release-jar", readonly = true, required = true)
    private File multiReleaseClasses;

    @Parameter(defaultValue = "${basedir}/src/main/java-mr")
    private File multiReleaseSourcesDirectory;

    @Parameter
    private String mainModuleInfo;

    @Override protected File getClassesDirectory() {
        return multiReleaseClasses;
    }

    @Override public void execute() throws MojoExecutionException {
        if (!multiReleaseSourcesDirectory.exists()) {
            super.execute();
            return;
        }

        if (!multiReleaseClasses.mkdirs()) {
            throw new MojoExecutionException(
                    "Failed to create the directory for multi-release-jar: " + multiReleaseClasses);
        }

        try {
            FileUtils.copyDirectoryStructure(buildOutputDirectory, multiReleaseClasses);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to copy " + buildOutputDirectory + " to " + multiReleaseClasses + ".", e);
        }

        if (mainModuleInfo != null) {
            File sourceModuleInfo = new File(CompileMojo.getOutputDirectory(buildOutputDirectory, mainModuleInfo), "module-info.class");
            File targetModuleInfo = new File(multiReleaseClasses, "module-info.class");
            try {
                Files.move(sourceModuleInfo.toPath(), targetModuleInfo.toPath());
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "Failed to move module-info.class from " + sourceModuleInfo + " to " + targetModuleInfo, e);
            }
        }

        //noinspection ConstantConditions
        for (File mrBase : multiReleaseSourcesDirectory.listFiles(File::isDirectory)) {
            String release = mrBase.getName();

            File releaseOutput = CompileMojo.getOutputDirectory(buildOutputDirectory, release);

            try {
                FileUtils.copyDirectoryStructure(releaseOutput, new File(multiReleaseClasses, "META-INF/versions/" + release));
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to copy " + releaseOutput + " to " + multiReleaseClasses + ".", e);
            }
        }

        super.execute();
    }
}
