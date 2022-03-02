package edu.lu.uni.serval.ibir.utils;

import edu.lu.uni.serval.ibir.config.Configuration;
import edu.lu.uni.serval.tbar.utils.JunitRunner;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class PathUtils {

    private static final List<String> D4J_PATHS_KEYS = new ArrayList<String>() {{
        add("dir.bin.classes");
        add("dir.bin.tests");
        add("dir.src.classes");
        add("dir.src.tests");
    }};

    private static final Map<String, List<String>> srcPaths = new HashMap<>();

    public static List<String> getSrcPath(String bugProject, String defects4jPath, String repoPath) {
        List<String> path = srcPaths.get(bugProject);
        if (path == null) {
            path = new ArrayList<>();
            String[] words = bugProject.split("_");
            for (String d4jPathsKey : D4J_PATHS_KEYS) {
                String shellResult = TestUtils.getDefects4jResult(repoPath, defects4jPath, "export -p " + d4jPathsKey, Configuration.SHELL_RUN_TIMEOUT_SEC);
                path.add(String.format("/%s/",shellResult.split("\n")[0].trim()));
            }
            srcPaths.put(bugProject, path);
        }
        return path;
    }

    public static String getJunitPath() {
        return System.getProperty("user.dir") + "/target/dependency/junit-4.12.jar";
    }

    private static String getHamcrestPath() {
        return System.getProperty("user.dir") + "/target/dependency/hamcrest-all-1.3.jar";
    }

    public static String buildCompileClassPath(List<String> additionalPath, String classPath, String testClassPath) {
        String path = "\"";
        path += classPath;
        path += System.getProperty("path.separator");
        path += testClassPath;
        path += System.getProperty("path.separator");
        path += JunitRunner.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        path += System.getProperty("path.separator");
        path += StringUtils.join(additionalPath, System.getProperty("path.separator"));
        path += "\"";
        return path;
    }

    public static String buildTestClassPath(String classPath, String testClassPath) {
        String path = "\"";
        path += classPath;
        path += System.getProperty("path.separator");
        path += testClassPath;
        path += System.getProperty("path.separator");
        path += JunitRunner.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        path += System.getProperty("path.separator");
        path += getJunitPath();
        path += System.getProperty("path.separator");
        path += getHamcrestPath();
        path += System.getProperty("path.separator");
        path += "\"";
        return path;
    }

}
