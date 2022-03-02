package edu.lu.uni.serval.ibir.utils;

import edu.lu.uni.serval.ibir.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

public final class GitUtils {
    private static Logger log = LoggerFactory.getLogger(GitUtils.class);

    private GitUtils() {
        throw new IllegalAccessError("Utility class: No instance allowed, static access only.");
    }

    public static void checkoutProjectRepoHead(String projectRepoPath) throws IOException {
        log.info("checkoutProjectRepoHead = " + projectRepoPath);
        String project = projectRepoPath.substring(projectRepoPath.lastIndexOf("/") + 1);
        String cmdStr = "git --work-tree=\"" + projectRepoPath + "\" --git-dir=\"" + projectRepoPath + "/.git\" checkout -- .";
        log.info("checkoutProjectRepoHead = " + cmdStr);
        ShellUtils.shellRun(Arrays.asList(cmdStr), project, Configuration.SHELL_RUN_TIMEOUT_SEC);
    }
}
