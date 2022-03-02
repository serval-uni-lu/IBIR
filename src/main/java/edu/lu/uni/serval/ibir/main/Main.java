package edu.lu.uni.serval.ibir.main;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;
import edu.lu.uni.serval.ibir.InjectionAbortingException;
import edu.lu.uni.serval.ibir.IbirInjector;
import edu.lu.uni.serval.ibir.config.Configuration;
import edu.lu.uni.serval.ibir.patterns.InstanceOfRemover;
import edu.lu.uni.serval.ibir.shell.EnvironmentPath;
import edu.lu.uni.serval.ibir.utils.GitUtils;
import edu.lu.uni.serval.ibir.utils.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static edu.lu.uni.serval.ibir.IbirInjector.NODE_SELECTION;
import static edu.lu.uni.serval.ibir.utils.TestUtils.loadTestsBrokenByOriginalBug;

/**
 * inject bugs with fix Localization results.
 *
 */
public class Main {
    public static final String D4J_INFOS_DIR = System.getProperty("D4J_INFOS_DIR", "input/d4j_v2/evaluation/bugs");
    private static Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length < 7) {
            System.out.println("Arguments: <Suspicious_Code_Positions_File_Path> <Fixed_Project_Path> <defects4j_Path> <output_path> <nbre_Mutants> <Project_Name> <java_home>");
            System.exit(1);
        }
        Configuration.bugInjectionTargetFilePath = args[0]; // $PROJECT_DIR$/stmtLoc/CLOSURE
        String fixedBugProjectsPath = args[1];// $PROJECT_DIR$/D4J/projects/
        String defects4jPath = args[2]; // $PROJECT_DIR$/D4J/defects4j/
        Configuration.outputPath = args[3]; // output/
        Configuration.mutantsNumber = Integer.valueOf(args[4]); // 2
        EnvironmentPath.JAVA_HOME = args[5]; // /Library/Java/JavaVirtualMachines/jdk1.8.0_212.jdk/Contents/Home
        String projectName_bugId = args[6]; // Lang_59
        if (args.length >= 8) {
            NODE_SELECTION = IbirInjector.LocalisationSelection.valueOf(args[7]);
        }
        if (args.length >= 9) {
            IbirInjector.PATTERNS = IbirInjector.Patterns.valueOf(args[8]);
        }
        Configuration.outputPath = Configuration.outputPath + "_locations_" + NODE_SELECTION + "_patterns_" + IbirInjector.PATTERNS.name()+"/";
        log.info("Injection project = " + projectName_bugId);
        log.info("Injection NODE_SELECTION mode = " + NODE_SELECTION);
        log.info("Injection PATTERNS mode = " + IbirInjector.PATTERNS);
        log.info("Injection PATTERNS with extra removing ones = " + InstanceOfRemover.IOR_REMOVER_ENABLED);
        log.info("outputPath = " + Configuration.outputPath);
        log.info("localisation file dir = " + Configuration.bugInjectionTargetFilePath);
        log.info("localisation file = " + IbirInjector.BUG_LOCALISATION_FILE);

        File f = new File(fixedBugProjectsPath + projectName_bugId + "/all_tests"); // if the all-tests.txt don't exist it will be generated here.
        if (!f.isFile()) {
            log.debug("No all-tests file found, testing with defects4j");
            TestUtils.testProjectWithDefects4j(fixedBugProjectsPath + projectName_bugId, defects4jPath);
        }

        try {
            GitUtils.checkoutProjectRepoHead(fixedBugProjectsPath + projectName_bugId);
            log.info("----- finished checking out head");
            TestUtils.compileProjectWithDefects4j(fixedBugProjectsPath + projectName_bugId, defects4jPath);
            injectBug(fixedBugProjectsPath, defects4jPath, projectName_bugId, Configuration.mutantsNumber, D4J_INFOS_DIR);
            System.exit(0);
        } catch (Throwable e) {
            log.error("injection issue ", e);
            System.exit(2);
        }
    }

    public static void injectBug(String bugFixedProjectPath, String defects4jPath, String projectName_BugId, int numberOfMutants, String d4jBugInfosDir) throws InjectionAbortingException {
        String suspiciousFileStr = Configuration.bugInjectionTargetFilePath;

        String[] elements = projectName_BugId.split("_");
        String projectName = elements[0];
        int bugId;
        try {
            bugId = Integer.valueOf(elements[1]);
        } catch (NumberFormatException e) {
            throw new InjectionAbortingException("Please input correct buggy project ID, such as \"Math_1\". in project:" + projectName_BugId);
        }

        IbirInjector ibirInjector;
        try {
            Set<String> testsBrokenByOriginalBug = loadTestsBrokenByOriginalBug(Paths.get(d4jBugInfosDir, String.format("%s_bugs.csv", projectName)).toAbsolutePath().toString(), bugId);
            ibirInjector = new IbirInjector(bugFixedProjectPath, projectName, bugId, defects4jPath, numberOfMutants, testsBrokenByOriginalBug);
        } catch (InterruptedException | ExecutionException | CsvValidationException | IOException e) {
            throw new InjectionAbortingException(e);
        }

        try {
            ibirInjector.readTestCases();
        } catch (IOException e) {
            log.warn("reading tests has thrown an IOException in project: " + projectName_BugId, e);
        }
        ibirInjector.countFailingTests();
        if (ibirInjector.hasFailingTests()) {
            throw new InjectionAbortingException("TS has failing tests in project: " + projectName_BugId);
        }
        ibirInjector.suspCodePosFile = new File(suspiciousFileStr);

        try {
            ibirInjector.injectProcess();
        } catch (IOException | CsvValidationException e) {
            throw new InjectionAbortingException(e);
        }
    }

}
