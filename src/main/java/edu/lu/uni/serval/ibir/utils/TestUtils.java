package edu.lu.uni.serval.ibir.utils;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;
import edu.lu.uni.serval.ibir.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownServiceException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;

public class TestUtils {

    /**
     * The possible values of the method_selection command-line argument.
     */
    public enum RandoopMethodSelectionMode {
        /**
         * Select methods randomly with uniform probability.
         */
        UNIFORM,
        /**
         * The "Bloodhound" technique from the GRT paper prioritizes methods with lower branch coverage.
         */
        BLOODHOUND;

        public static RandoopMethodSelectionMode forVal(String method_selection_mode) {
            for (RandoopMethodSelectionMode value : values()) {
                if (value.name().equalsIgnoreCase(method_selection_mode)) return value;
            }
            return null;
        }
    }

    private static Logger log = LoggerFactory.getLogger(TestUtils.class);

    private static final String DEFECTS4J_FAILING_TEST_OUTPUT_REGEX = "\\s+-\\s.+"; // i.e.   - org.apache.commons.lang.text.StrBuilderAppendInsertTest::testAppendFixedWidthPadRight_int
    public static final Pattern DEFECTS4J_FAILING_TEST_OUTPUT_PATTERN = Pattern.compile(DEFECTS4J_FAILING_TEST_OUTPUT_REGEX);

    public static boolean isFailingTestLine(String line) {
        return line != null && line.length() > 0 && DEFECTS4J_FAILING_TEST_OUTPUT_PATTERN.matcher(line).matches();
    }


    public static Set<String> loadTestsBrokenByOriginalBug(String csvPath, int bugId) throws CsvValidationException, IOException {
        final String BUG_ID_KEY = "bug.id";
        final String BROKEN_TESTS_KEY = "tests.trigger";
        CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(csvPath));
        String bugIdStr = String.valueOf(bugId);
        Map<String, String> bugInfos;
        do {
            bugInfos = reader.readMap();
        } while (!bugIdStr.equals(bugInfos.get(BUG_ID_KEY)));

        String res = bugInfos.get(BROKEN_TESTS_KEY);
        return new HashSet<String>() {{
            addAll(Arrays.asList(res.split(";")));
        }};
    }

    public static Set<String> readTestCases(String fullBuggyProjectPath, String defects4jPath, String...forceReloadMatching) throws IOException {
        FileReader fis = null;
        BufferedReader reader = null;
        Set<String> allTestCases= new HashSet<>();
        try {
            File testCasesFile = new File(fullBuggyProjectPath + "/all_tests");
            if (!testCasesFile.exists())
                TestUtils.testAllTestCases(fullBuggyProjectPath, defects4jPath, new ArrayList<String>());
            else if (forceReloadMatching!= null && forceReloadMatching.length != 0){
               boolean regenAllTests = false;
                for (String s : forceReloadMatching) {
                    regenAllTests = fullBuggyProjectPath.contains(s);
                    if (regenAllTests) break;
                }
                if (regenAllTests){
                    Files.deleteIfExists(testCasesFile.toPath());
                    TestUtils.testAllTestCases(fullBuggyProjectPath, defects4jPath, new ArrayList<String>());
                }
            }
            fis = new FileReader(testCasesFile);
            reader = new BufferedReader(fis);
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] elements = line.split("\\(|\\)");
                String testCase = elements[1] + "::" + elements[0];
                allTestCases.add(testCase);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignored) {
                }
            }
        }
        assert allTestCases.size() > 1;
        return allTestCases;
    }

    public static int testAllTestCases(String projectName, String defects4jPath, List<String> failedTests) {
        String testResult = getDefects4jResult(projectName, defects4jPath, "test", Configuration.SHELL_RUN_TIMEOUT_SEC);
        log.info(testResult);
        if (testResult.equals("")) {//error occurs in run
            return Integer.MAX_VALUE;
        }
        int errorNum = 0;
        String[] lines = testResult.trim().split("\n");
        for (String lineString : lines) {
            if (lineString.startsWith("Failing tests:")) {
                errorNum = Integer.valueOf(lineString.split(":")[1].trim());
                if (errorNum == 0) break;
            } else if (lineString.startsWith("Running ")) {
                break;
            } else {
                failedTests.add(lineString.trim().substring(1).trim());
            }
        }
        return errorNum;
    }

    /**
     * todo clean this dirt: that extraArgs param is so smelly :p
     */
    public static Set<String> testAllTestCases(String fullBuggyProjectPath, String defects4jPath, String... extraArgs) throws UnknownServiceException {
        Set<String> failedTests = new HashSet<>();
        StringBuilder cmdType = new StringBuilder("test");
        if (extraArgs != null && extraArgs.length > 0) {
            for (String extraArg : extraArgs) {
                cmdType.append(" ").append(extraArg);
            }
        }
        String testResult = getDefects4jResult(fullBuggyProjectPath, defects4jPath, cmdType.toString(), Configuration.SHELL_RUN_TIMEOUT_SEC);
        log.info(testResult);
        if (testResult.equals("")) {
            throw new UnknownServiceException("tests run failed");
        }
        int errorNum = 0;
        String[] lines = testResult.trim().split("\n");
        for (String lineString : lines) {
            if (lineString.startsWith("Failing tests:")) {
                errorNum = Integer.valueOf(lineString.split(":")[1].trim());
                log.info("failing tests count = " + errorNum);
                if (errorNum == 0) return failedTests; // exit if no failing tests.
            } else if (isFailingTestLine(lineString)) {
                failedTests.add(lineString.replace("- ", "").trim());
            }
        }
        // In some cases, we got the same test printed twice - therefore  the errorNum was higher than the size of failedTests.
        assert errorNum >= failedTests.size() : "the printed error number [" + errorNum + "] is different from the failing tests count [" + failedTests.size() + "]";
        return failedTests;
    }

    public static int testProjectWithDefects4j(String projectName, String defects4jPath) {
        String compileResults = getDefects4jResult(projectName, defects4jPath, "test", Configuration.SHELL_RUN_TIMEOUT_SEC);
        String[] lines = compileResults.split("\n");
        if (lines.length != 2) return 1;
        for (String lineString : lines) {
            if (!lineString.endsWith("OK")) return 1;
        }
        return 0;
    }

    public static int compileProjectWithDefects4j(String projectName, String defects4jPath) {
        String compileResults = getDefects4jResult(projectName, defects4jPath, "compile", Configuration.SHELL_RUN_TIMEOUT_SEC);
        String[] lines = compileResults.split("\n");
        if (lines.length != 2) return 1;
        for (String lineString : lines) {
            if (!lineString.endsWith("OK")) return 1;
        }
        return 0;
    }


    public static String getDefects4jResult(String projectFullPath, String defects4jPath, String cmdType, long secTimout) {
        try {
            String buggyProject = projectFullPath.substring(projectFullPath.lastIndexOf("/") + 1);
            String result = ShellUtils.shellRun(Arrays.asList(defects4jPath + "framework/bin/defects4j " + cmdType + " -w " + projectFullPath + "\n"), buggyProject, secTimout);// "defects4j " + cmdType + "\n"));//
            log.info(result);
            return result.trim();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String readPatch(String projectName) {
        try {
            String buggyProject = projectName.substring(projectName.lastIndexOf("/") + 1);
            return ShellUtils.shellRun(Arrays.asList("cd " + projectName + "\n", "git diff"), buggyProject, Configuration.SHELL_RUN_TIMEOUT_SEC);
        } catch (IOException e) {
            return null;
        }
    }

    public static String readTestResult(String result) {
        String failedTeatCase = null;
        String[] testResults = result.split("\n");
        for (String testResult : testResults) {
            testResult = testResult.trim();
            if (testResult.isEmpty()) continue;
            if ("Failing tests: 0".equals(testResult)) {
                failedTeatCase = "";
                break;
            }
            if ("Failing tests: 1".equals(testResult)) continue;
            if (testResult.trim().startsWith("-")) {
                failedTeatCase = testResult.substring(1).trim();
                break;
            }
        }
        return failedTeatCase;
    }

}
