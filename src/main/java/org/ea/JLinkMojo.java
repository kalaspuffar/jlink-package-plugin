package org.ea;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Goal which touches a timestamp file.
 */
@Mojo( name = "jlink-package", defaultPhase = LifecyclePhase.PACKAGE )
public class JLinkMojo extends AbstractMojo {
    @Parameter( defaultValue = "${project.build.directory}", property = "outputDir", required = true )
    private File outputDirectory;

    @Parameter( defaultValue = "${project.build.directory}", property = "buildDir", required = true )
    private File buildDirectory;

    @Parameter( defaultValue = "${java.home}", property = "javaHome", required = true )
    private File javaHome;


    @Parameter( defaultValue = "${project.name}", property = "projectName", required = true )
    private String projectName;

    @Parameter( defaultValue = "${project.version}", property = "projectVersion", required = true )
    private String projectVersion;


    @Parameter( property = "mainClass", required = true )
    private String mainClass;

    @Parameter( property = "descriptorRef", required = true )
    private String descriptorRef;

    public void execute() throws MojoExecutionException {
        try {
            StringBuilder jarFilePath = new StringBuilder();
            jarFilePath.append(buildDirectory.getAbsolutePath());
            jarFilePath.append(File.separator);
            jarFilePath.append(projectName);
            jarFilePath.append("-");
            jarFilePath.append(projectVersion);
            jarFilePath.append("-");
            jarFilePath.append(descriptorRef);
            jarFilePath.append(".jar");

            projectName = projectName.replaceAll("-", ".");

            getLog().info(jarFilePath.toString());

            StringBuilder jdepsSB = new StringBuilder();
            jdepsSB.append(javaHome);
            jdepsSB.append(File.separator);
            jdepsSB.append("bin");
            jdepsSB.append(File.separator);
            jdepsSB.append("jdeps --print-module-deps ");
            jdepsSB.append(jarFilePath);

            getLog().info(jdepsSB.toString());

            Process p = Runtime.getRuntime().exec(jdepsSB.toString());

            String s;
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder jdepsOutput = new StringBuilder();
            while ((s = stdInput.readLine()) != null) {
                jdepsOutput.append(s);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("jlink ");
            sb.append("--module-path ");
            sb.append(javaHome);
            sb.append(File.separator);
            sb.append("jmods");
            sb.append(File.pathSeparator).append(jarFilePath);
            sb.append(" --add-modules ");
            sb.append(projectName).append(",");
            sb.append(jdepsOutput.toString().replaceAll("\\s", ""));
            sb.append(" --output ");
            sb.append(outputDirectory).append("/image ");
            sb.append(" --launcher start=" + projectName + "/" + mainClass);

            getLog().info(sb.toString());

            p = Runtime.getRuntime().exec(sb.toString());

            stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            // read the output from the command
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }
            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));
            // read any errors from the attempted command
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }

            FileOutputStream fos = new FileOutputStream(outputDirectory.getAbsolutePath() + "/" + projectName + ".zip");
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            File fileToZip = new File(outputDirectory, "image");

            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, childFile.getName(), zipOut);
            }
            zipOut.close();
            fos.close();

            delete(fileToZip);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void delete(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
        if (!f.delete())
            throw new FileNotFoundException("Failed to delete file: " + f);
    }

    private void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

}
