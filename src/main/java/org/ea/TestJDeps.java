package org.ea;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TestJDeps {
    public static void main(String[] args) {
        try {
            String jarFilePath = "C:\\github\\jlink-example\\target\\jlink-example-0.0.1-SNAPSHOT-jar-with-dependencies.jar";
            String javaHome = "C:\\Java\\jdk-12.0.1";
            String projectName = "jlink.example";
            String execClass = "org.ea.JLinkExample";
            String TMP_DIR = "image2";

            StringBuilder jdepsSB = new StringBuilder();
            jdepsSB.append(javaHome);
            jdepsSB.append("\\bin\\jdeps --print-module-deps ");
            jdepsSB.append(jarFilePath);

            Process p = Runtime.getRuntime().exec(jdepsSB.toString());

            String s = null;
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder jdepsOutput = new StringBuilder();
            while ((s = stdInput.readLine()) != null) {
                jdepsOutput.append(s);
            }

            StringBuilder sb = new StringBuilder();
            sb.append(javaHome);
            sb.append("\\bin\\jlink ");
            sb.append("--module-path ");
            sb.append(javaHome);
            sb.append("\\jmods");
            sb.append(";").append(jarFilePath);
            sb.append(" --add-modules ");
            sb.append(projectName).append(",");
            sb.append(jdepsOutput.toString().replaceAll("\\s", ""));
            sb.append(" --output ");
            sb.append(TMP_DIR).append(" ");
            sb.append(" --launcher start=" + projectName + "/" + execClass);

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

            FileOutputStream fos = new FileOutputStream(projectName + ".zip");
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            File fileToZip = new File(TMP_DIR);

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

    private static void delete(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
        if (!f.delete())
            throw new FileNotFoundException("Failed to delete file: " + f);
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
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
