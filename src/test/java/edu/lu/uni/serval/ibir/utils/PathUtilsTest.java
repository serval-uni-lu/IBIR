package edu.lu.uni.serval.ibir.utils;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class PathUtilsTest {


    private static final Path D4J_REPOS = Paths.get("PATH_TO_CLOSURE");

    @Test
    public void getSrcPathTest() {
        if (!D4J_REPOS.toFile().isDirectory()) {
            System.err.println("skipping test ! you need to define the absolute path to the directory containing Colsure_1 repo! ");
            return;
        }
        List<String> expected = Arrays.asList("/build/classes/", "/build/test/", "/src/", "/test/");
        String bugProject = "Closure_1", defects4jPath = "D4J/defects4j/", repoPath = D4J_REPOS.resolve("Closure_1").toString();
        List<String> srcPaths = PathUtils.getSrcPath(bugProject, defects4jPath, repoPath);
        assertNotNull(srcPaths);
        assertFalse(srcPaths.isEmpty());
        assertEquals(expected, srcPaths);
    }
}