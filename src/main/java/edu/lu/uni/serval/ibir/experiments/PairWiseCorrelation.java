package edu.lu.uni.serval.ibir.experiments;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;
import edu.lu.uni.serval.ibir.main.Main;
import edu.lu.uni.serval.ibir.shell.EnvironmentPath;
import edu.lu.uni.serval.ibir.utils.FileUtils;
import edu.lu.uni.serval.ibir.utils.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static edu.lu.uni.serval.ibir.utils.TestUtils.loadTestsBrokenByOriginalBug;

/**
 * java -ea -cp release/IBIR-1.1.5-SNAPSHOT-jar-with-dependencies.jar edu.lu.uni.serval.ibir.experiments.PairWiseCorrelation
 *
 * todo read ts size directly from ts_sizes.csv
 */
public class PairWiseCorrelation extends TestSuiteSamplingCorrelation {

    private final static String ALL_PROJECTS_MODE = "ALL";
    private static final String ARRAY_OF_PROJECTS_PREFIX = "arr:";
    private static final String BROKEN_TESTS_COLUMN = "brokenTests";
    private static Logger log = LoggerFactory.getLogger(PairWiseCorrelation.class);
    private static final String MUTATION_MATRIX_FILE_END = "_mat.csv";

    private static final String PATCH_ID_COLUMN = "patchId";
    private static final boolean EXCLUDE_ALREADY_GENERATED = true;

    // todo refactor: the user passes whatever names mapped with the mat dir paths.
    protected static final String RAND_DEF_KEY = "rand";
    private static final List<String> DEF_TOOL_KEYS = new ArrayList<String>() {{
        add(IBIR_DEF_KEY);
        add(RAND_DEF_KEY);
    }};

    private static String[] parse(String target) {
        if (target.startsWith(ARRAY_OF_PROJECTS_PREFIX)) {
            return target.substring(ARRAY_OF_PROJECTS_PREFIX.length()).split(",");
        }
        return new String[]{target};
    }

    public static void main(String... args) {
        try {
            String[] targets = args.length > 0 ? parse(args[0]) : new String[]{ALL_PROJECTS_MODE}; // i.e. Cli_1
            for (String target : targets) {
                if (target != null && !target.isEmpty()) {
                    internalMain(target, args);
                }
            }
        } catch (Throwable throwable) {
            log.error("-- FAIL --", throwable);
            System.exit(1);
        }
    }

    public static void internalMain(String projectName, String... args) throws IOException, CsvValidationException {

        String projectsReposPath = args.length > 1 ? args[1] : "/Volumes/AhmedKhanfi/Experiments/ibir/defects4jdata/f";
        String ibirMutationMatPath = args.length > 2 ? args[2] : "tosem_results_v17/_locations_IBIR_patterns_IBIR/ibir_mutation_mat";
        String randMutationMatPath = args.length > 3 ? args[3] : "tosem_results_v17/_locations_RANDOM_patterns_RANDOM/ibir_mutation_mat";
        String defects4jPath = args.length > 4 ? args[4] : "D4J/defects4j/";
        String outPutDir = args.length > 5 ? args[5] : "tosem_results_v17/ibir_rand_correlation_seed10";
        String javaHome = args.length > 6 ? args[6] : "/Library/Java/JavaVirtualMachines/jdk1.8.0_212.jdk/Contents/Home";
        int mutantsNumber = args.length > 7 ? Integer.parseInt(args[7]) : 100;

        EnvironmentPath.JAVA_HOME = javaHome;
        // create output dir.
        String outputDirPath = Files.createDirectories(Paths.get(outPutDir).toAbsolutePath()).toString();

        File ibirMutationMatDir = new File(ibirMutationMatPath);
        File randMutationMatDir = new File(randMutationMatPath);
        assert ibirMutationMatDir.exists() && ibirMutationMatDir.isDirectory() : "wrong ibirMutationMatPath " + ibirMutationMatPath;
        assert randMutationMatDir.exists() && randMutationMatDir.isDirectory() : "wrong randMutationMatPath " + randMutationMatPath;

        if (ALL_PROJECTS_MODE.equals(projectName)) {
            int[] count = {0, 0};

            Files.list(ibirMutationMatDir.toPath()).filter(f -> f.toString().endsWith(MUTATION_MATRIX_FILE_END)).forEach(f -> {
                try {
                    log.info("----------- parsing " + f.getFileName());
                    String pName = f.getFileName().toString().split(MUTATION_MATRIX_FILE_END)[0];
                    readAllLinesCalculateCorrelation(pName, f, randMutationMatPath, projectsReposPath, defects4jPath, mutantsNumber, outputDirPath);
                    count[0]++;
                    log.info("---- total parsed projects = success " + count[0] + " | failure " + count[1]);
                } catch (IOException | CsvValidationException e) {
                    count[1]++;
                    log.error("parsing failed for : " + f, e);
                }
            });
        } else {
            readAllLinesCalculateCorrelation(projectName, Paths.get(ibirMutationMatPath, projectName + MUTATION_MATRIX_FILE_END), randMutationMatPath, projectsReposPath, defects4jPath, mutantsNumber, outputDirPath);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("project,project_bugId,ts_count\n");
        for (String project : projectTsSize.keySet()) {
            Map<String, Integer> entry = projectTsSize.get(project);
            for (String project_bugId : entry.keySet()) {
                Integer ts_count = entry.get(project_bugId);
                stringBuilder.append(project).append(",").append(project_bugId).append(",").append(ts_count).append("\n");
            }
        }
        FileUtils.createFile(Paths.get(outputDirPath, "ts_sizes.csv").toFile(), stringBuilder.toString());
    }

    private static Map<String, Map<String, Integer>> projectTsSize = new HashMap<>();


    private static void readAllLinesCalculateCorrelation(String projectName, Path ibirMatrix, String randMutationMatDirPath,
                                                         String projectsReposPath, String defects4jPath,
                                                         int mutantsNumber, String outputDirPath) throws IOException, CsvValidationException {
        String[] forceReload = null;//{"Math", "Closure"};
        boolean hasResults = TestSuiteSamplingCorrelation.hasResults(projectName, DEF_TOOL_KEYS, outputDirPath, mutantsNumber, forceReload);
        if (hasResults) {
            log.info("skipped: found generated results .... " + projectName);
            return;
        }

        String[] splits = projectName.split("_");
        int bugId = Integer.valueOf(splits[1]);
        String pName = splits[0];

        File ibirF = ibirMatrix.toFile();
        File randF = Paths.get(randMutationMatDirPath, ibirMatrix.getFileName().toString()).toFile();

        CSVReaderHeaderAware ibirReader = new CSVReaderHeaderAware(new FileReader(ibirF));
        CSVReaderHeaderAware randReader = new CSVReaderHeaderAware(new FileReader(randF));
        Set<String> testsBrokenByOriginalBug = loadTestsBrokenByOriginalBug(Paths.get(Main.D4J_INFOS_DIR, String.format("%s_bugs.csv", pName)).toAbsolutePath().toString(), bugId);
        TestSuiteSamplingCorrelation testSuiteSamplingCorrelation = null;
        try {
            int count = TestUtils.readTestCases(Paths.get(projectsReposPath, projectName).toString(), defects4jPath, forceReload).size();
            log.info("ts size " + count);
            if (projectTsSize.get(pName) == null) {
                projectTsSize.put(pName, new HashMap<String, Integer>() {{
                    put(projectName, count);
                }});
            } else {
                projectTsSize.get(pName).put(projectName, count);
            }

            testSuiteSamplingCorrelation = new PairWiseCorrelation(DEF_TOOL_KEYS, projectName, count, testsBrokenByOriginalBug);
        } catch (Throwable throwable) {
            log.error("failed ......................... ", throwable);
            return;
        }

        boolean stopReadingFile = false;
        List<Map<String, String>> ibirMutants = new ArrayList<>();
        Map<String, String> ibirMap = ibirReader.readMap();
        while (ibirMap != null) {
            ibirMutants.add(ibirMap);
            ibirMap = ibirReader.readMap();
        }
        if (ibirMutants.isEmpty()) {
            log.error("no ibir mutant loaded for : " + projectName);
            return;
        }

        int linesRead = 0;
        while (!stopReadingFile && linesRead < mutantsNumber && linesRead < ibirMutants.size()) {
            log.info("line: " + linesRead);
            ibirMap = ibirMutants.get(linesRead);
            if (ibirMap != null) {
                Map<String, String> randMap = randReader.readMap();
                if (randMap != null) {
                    Set<String> brokenTestsByIbirMutant = loadBrokenTests(ibirMap);
                    Set<String> brokenTestsByRandMutant = loadBrokenTests(randMap);
                    Map<String, Collection<String>> brokenTestsByMutant = new HashMap<String, Collection<String>>() {{
                        put(IBIR_DEF_KEY, brokenTestsByIbirMutant);
                        put(RAND_DEF_KEY, brokenTestsByRandMutant);
                    }};
                    testSuiteSamplingCorrelation.addMutantResults(brokenTestsByMutant);
                    linesRead++;
                } else {
                    stopReadingFile = true;
                }
            } else {
                stopReadingFile = true;
            }
        }
        testSuiteSamplingCorrelation.printCorrelationMatrices(outputDirPath, mutantsNumber);
    }

    private static Set<String> loadBrokenTests(Map<String, String> mutant) {
        String brokenTests = mutant.get(BROKEN_TESTS_COLUMN);
        if (brokenTests == null || brokenTests.trim().isEmpty()) return null;
        return new HashSet<>(Arrays.asList(brokenTests.split(" ")));
    }


    public PairWiseCorrelation(List toolKeys, String projectBugId, int allTestSuiteCount, Set<String> brokenTestByBug) {
        super(SAMPLES_COUNT, CORRELATION_SAMPLE_RATIO_RANGE, toolKeys, projectBugId, allTestSuiteCount, brokenTestByBug);
    }

}
