package edu.lu.uni.serval.ibir.utils;

import edu.lu.uni.serval.ibir.config.Configuration;

import java.io.*;

public class FileUtils {

    public static File createFile(File file, String content) throws IOException {
        file = createFile(file,false);
        writeStringToFile(file,content);
        return file;
    }

    public static File createFile(String filePath, boolean directory) throws IOException {
        if (filePath == null || filePath.isEmpty())
            throw new IllegalArgumentException("filePath is empty or null");
        File file = new File(filePath);
        return createFile(file,directory);
    }

    public static File createFile(File file, boolean directory) throws IOException {
        if (!file.exists()) {
            if (file.getParentFile() != null && !file.getParentFile().exists() && !file.getParentFile().mkdirs())
                throw new IOException("mkdirs failed to create abstract path directories");
            if (directory && !file.mkdir())
                throw new IOException("trying to create an existing directory. It could be caused by a multithreading. Path = " + file.getAbsolutePath());
            else if (!directory && !file.createNewFile())
                throw new IOException("trying to create an existing file. It could be caused by a multithreading. Path = " + file.getAbsolutePath());
        }
        return file;
    }

    public static int fileLines(String filePath) throws IOException {
        int result;
        try (
                FileReader input = new FileReader(filePath);
                LineNumberReader count = new LineNumberReader(input);
        ) {
            while (count.skip(Long.MAX_VALUE) > 0) {
                // Loop just in case the file is > Long.MAX_VALUE or skip() decides to not read the entire file
            }
            result = count.getLineNumber();     // +1 because line index starts at 0
        }
        return result;
    }

    public static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }


    public static void writeStringToFile(File file, String fileContent) throws IOException {
        if (file == null || !file.exists() || fileContent == null || fileContent.isEmpty())
            throw new IllegalArgumentException("file is null or do not exist or fileContent is null or empty");
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file))) {
            bufferedWriter.write(fileContent);
            bufferedWriter.flush();
        }
    }

    public static String tempJavaPath(String classname, String identifier) {
        new File(Configuration.TEMP_FILES_PATH + identifier).mkdirs();
        return Configuration.TEMP_FILES_PATH + identifier + "/" + classname.replace("/", "_");
    }

    public static String tempClassPath(String classname, String identifier) {
        new File(Configuration.TEMP_FILES_PATH + identifier).mkdirs();
        return Configuration.TEMP_FILES_PATH + identifier + "/" + classname.replace("/", "_").replace(".java", ".class");
    }

    public static void appendLineToFile(File file, String line) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(file, true))) {
            out.println(line);
        }
    }
}
