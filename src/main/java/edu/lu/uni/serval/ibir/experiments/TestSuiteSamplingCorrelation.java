package edu.lu.uni.serval.ibir.experiments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import static edu.lu.uni.serval.ibir.utils.FileUtils.createFile;
import static edu.lu.uni.serval.ibir.utils.FileUtils.writeStringToFile;


public class TestSuiteSamplingCorrelation {

    private static final Logger log = LoggerFactory.getLogger(TestSuiteSamplingCorrelation.class);


    protected static final float CORRELATION_SAMPLE_RATIO_MIN = 0.1f;
    protected static final float CORRELATION_SAMPLE_RATIO_MAX = 0.3f;
    protected static final float[] CORRELATION_SAMPLE_RATIO_RANGE = {CORRELATION_SAMPLE_RATIO_MIN, CORRELATION_SAMPLE_RATIO_MAX};
    protected static final int SAMPLES_COUNT = 50;
    protected static final List<Integer> MUTANTS_NUMBER_PERCENT = new ArrayList<Integer>() {{
        add(5);
        add(10);
        add(30);
        add(100);
    }};

    protected static final long TEST_IDS_SEED = 10L;

    protected static final String BUG_ID_COLUMN = "bugId";
    protected static final String MUTANTS_NUMBER_COLUMN = "mutantsNumber";
    protected static final String TS_ID_COLUMN = "testSuiteId";
    protected static final String IS_BUG_KILLED_COLUMN = "isBugKilled";
    protected static final String KILLED_MUTANTS_PERCENT_COLUMN = "percentageMutantsKilled";
    protected static final String KILLED_MUTANTS_COUNT_COLUMN = "countMutantsKilled";


    protected static final String IBIR_DEF_KEY = "ibir";

    protected final String projectBugId;
    protected final int allTestSuiteCount;
    protected final Map<Integer, RandomTestSuiteSample> randomTestSuiteSamples;
    protected final Map<String, Integer> testNameId;
    protected final Random random;

    public static final String[] HEADERS = {BUG_ID_COLUMN, MUTANTS_NUMBER_COLUMN, TS_ID_COLUMN, IS_BUG_KILLED_COLUMN, KILLED_MUTANTS_PERCENT_COLUMN, KILLED_MUTANTS_COUNT_COLUMN};
    private final HashMap<String, ResultsHolder> results;

    public TestSuiteSamplingCorrelation(int samplesCount, float[] sampleRatioRange, List<String> toolKeys, String projectBugId, int allTestSuiteCount, Set<String> brokenTestByBug) {
        this.projectBugId = projectBugId;
        this.allTestSuiteCount = allTestSuiteCount;
        this.randomTestSuiteSamples = new HashMap<>();
        this.testNameId = new HashMap<>();
        this.random = new Random(TEST_IDS_SEED);
        this.results = new HashMap<>();
        for (String toolKey : toolKeys) {
            this.results.put(toolKey, new ResultsHolder());
        }
        for (int testSuitId = 0; testSuitId < samplesCount; testSuitId++) {
            float sampleRatio;
            assert sampleRatioRange != null && sampleRatioRange.length > 0;
            if (sampleRatioRange.length == 1 || sampleRatioRange[0] == sampleRatioRange[1]) {
                sampleRatio = sampleRatioRange[0];
            } else {
                assert sampleRatioRange.length == 2;
                float min = sampleRatioRange[0], max = sampleRatioRange[1];
                assert min < max;
                sampleRatio = min + random.nextFloat() * (max - min);
                assert sampleRatio >= min;
                assert sampleRatio <= max;
            }
            int numberOfTests = Math.round(sampleRatio * (float) allTestSuiteCount);
            RandomTestSuiteSample randomTestSuiteSample = new RandomTestSuiteSample(testSuitId,random, numberOfTests, toolKeys);
            boolean bugIsKilled = randomTestSuiteSample.setIsBugKilled(brokenTestByBug);
            randomTestSuiteSamples.put(testSuitId, randomTestSuiteSample);
        }
    }

    public void addMutantResults(Map<String, Collection<String>> brokenTestsByMutant) {
        for (RandomTestSuiteSample value : randomTestSuiteSamples.values()) {
            value.addMutantResults(brokenTestsByMutant);
        }
    }

    public int getTestId(String testName) {
        if (testNameId.containsKey(testName)) {
            return testNameId.get(testName);
        }
        int max = allTestSuiteCount;
        int testId = random.nextInt(max);
        while (testNameId.containsValue(testId)) {
            int newMax = testId;
            while (testNameId.containsValue(testId) && testId < max - 1) {
                testId++;
            }
            if (testNameId.containsValue(testId)) {
                if (newMax >= 1) {
                    max = newMax;
                    testId = random.nextInt(max);
                } else {
                    testId = 0;
                }
            }
        }
        testNameId.put(testName, testId);
        return testId;
    }

    public List<Map<String, String>> getIbirCsvLines(String toolKey, List<Integer> mutantsNumbers) {
        List<Map<String, String>> result = new ArrayList<>();
        for (RandomTestSuiteSample randomTestSuiteSample : randomTestSuiteSamples.values()) {
            result.addAll(randomTestSuiteSample.getIbirCsvLines(toolKey, mutantsNumbers));
        }
        return result;
    }


    public List<Map<String, String>> getIbirFoundCsvLines(String toolKey, List<Integer> mutantsNumbers) {
        List<Map<String, String>> result = new ArrayList<>();
        for (RandomTestSuiteSample randomTestSuiteSample : randomTestSuiteSamples.values()) {
            if (randomTestSuiteSample.isBugKilled)
                result.addAll(randomTestSuiteSample.getIbirCsvLines(toolKey, mutantsNumbers));
        }
        return result;
    }

    public List<Map<String, String>> getIbirNotFoundCsvLines(String toolKey, List<Integer> mutantsNumbers) {
        List<Map<String, String>> result = new ArrayList<>();
        for (RandomTestSuiteSample randomTestSuiteSample : randomTestSuiteSamples.values()) {
            if (!randomTestSuiteSample.isBugKilled)
                result.addAll(randomTestSuiteSample.getIbirCsvLines(toolKey, mutantsNumbers));
        }
        return result;
    }

    public void printCorrelationMatrices(String correlationMatricesPath, int reqMNbre) throws IOException {
        for (String toolKey : results.keySet()) {
            printCorrelationMatrices(toolKey, correlationMatricesPath, reqMNbre);
        }
    }

    public static Path getResultsDir(String toolKey, String correlationMatricesPath, int reqMNbre) {
        return Paths.get(correlationMatricesPath, String.valueOf(reqMNbre), CORRELATION_SAMPLE_RATIO_MIN + "_" + CORRELATION_SAMPLE_RATIO_MAX, toolKey);
    }

    public static boolean hasResults(String projectBugId, List<String> toolKeys, String correlationMatricesPath, int reqMNbre, String... forceReload) throws IOException {
        boolean result = true;
        if (forceReload != null && forceReload.length != 0) {
            for (String pName : forceReload) {
                if (projectBugId.contains(pName)) {
                    for (String toolKey : toolKeys) {
                        Set<File> resultFiles = getResultPaths(projectBugId, toolKey, correlationMatricesPath, reqMNbre);
                        for (File resultFile : resultFiles) {
                            Files.deleteIfExists(resultFile.toPath());
                        }
                    }
                    return false;
                }
            }
        }
        for (String toolKey : toolKeys) {
            if (!hasResults(projectBugId, toolKey, correlationMatricesPath, reqMNbre)) return false;
        }
        return result;
    }

    public static boolean hasResults(String projectBugId, String toolKey, String correlationMatricesPath, int reqMNbre) {
        boolean result = true;
        Set<File> resultFiles = getResultPaths(projectBugId, toolKey, correlationMatricesPath, reqMNbre);
        for (File resultPath : resultFiles) {
            if (!resultPath.exists()) return false;
        }
        return result;
    }

    public static Set<File> getResultPaths(String projectBugId, String toolKey, String correlationMatricesPath, int reqMNbre) {
        Path dir = getResultsDir(toolKey, correlationMatricesPath, reqMNbre);
        Set<String> filter = new HashSet<String>() {{
            add("_correlation_mat.csv");
            add("_FOUND_correlation_mat.csv");
            add("_NOT_FOUND_correlation_mat.csv");
        }};
        Set<File> result = new HashSet<>();
        for (String f : filter) {
            result.add(Paths.get(dir.toString(), projectBugId + f).toFile());
        }
        return result;
    }

    public void printCorrelationMatrices(String toolKey, String correlationMatricesPath, int reqMNbre) throws IOException {
        List<Integer> mutantsNumbers = getMutantsNumber(reqMNbre);
        ResultsHolder resultHolder = results.get(toolKey);
        resultHolder.getTssCorrelationCsv().addAll(getIbirCsvLines(toolKey, mutantsNumbers));
        resultHolder.getTssOriginalBugFoundCorrelationCsv().addAll(getIbirFoundCsvLines(toolKey, mutantsNumbers));
        resultHolder.getTssOriginalBugNotFoundCorrelationCsv().addAll(getIbirNotFoundCsvLines(toolKey, mutantsNumbers));
        Path dir = Files.createDirectories(getResultsDir(toolKey, correlationMatricesPath, reqMNbre));
        writeCsv(Paths.get(dir.toString(), projectBugId + "_correlation_mat.csv").toString(),
                resultHolder.getTssCorrelationCsv(), HEADERS, false);
        writeCsv(Paths.get(dir.toString(), projectBugId + "_FOUND_correlation_mat.csv").toString(),
                resultHolder.getTssOriginalBugFoundCorrelationCsv(), HEADERS, true);
        writeCsv(Paths.get(dir.toString(), projectBugId + "_NOT_FOUND_correlation_mat.csv").toString(),
                resultHolder.getTssOriginalBugNotFoundCorrelationCsv(), HEADERS, true);
    }

    protected List<Integer> getMutantsNumber(int reqMNbre) {
        if (reqMNbre == 100) return MUTANTS_NUMBER_PERCENT;
        List<Integer> result = new ArrayList<>();
        for (Integer percent : MUTANTS_NUMBER_PERCENT) {
            float mnf = ((float) (percent * reqMNbre)) / 100f;
            int mn = Math.round(mnf);
            if (mn > 0) {
                result.add(mn);
            }
        }
        return result;
    }


    public static void writeCsv(String path, List<Map<String, String>> csvData, String[] headers, boolean acceptNull) throws IOException {
        if (!acceptNull && (csvData == null || csvData.isEmpty()))
            throw new IllegalArgumentException("null or empty csvData!");
        if (headers == null || headers.length == 0) throw new IllegalArgumentException("null or empty headers!");
        File file = createFile(path, false);
        List<String[]> headerAndLines = new ArrayList<>();
        headerAndLines.add(headers);
        if (csvData != null && !csvData.isEmpty()) {
            for (Map<String, String> csvDatum : csvData) {
                String[] line = new String[headers.length];
                int i = 0;
                for (String header : headers) {
                    line[i] = csvDatum.get(header);
                    i++;
                }
                headerAndLines.add(line);
            }
        }
        StringBuilder sb = new StringBuilder();

        for (String[] line : headerAndLines) {
            sb.append(line[0]);
            for (int i = 1; i < line.length; i++) {
                sb.append(",");
                sb.append(line[i]);
            }
            lineBreak(sb);
        }

        writeStringToFile(file, sb.toString());
        log.info("wrote csv file : " + path);
    }


    /**
     * appends a line break to the passed StringBuilder.
     * PS: System.lineSeparator() is not used to avoid system dependant (portability) eventual issues.
     *
     * @param stringBuilder the string builder to append to.
     */
    public static void lineBreak(StringBuilder stringBuilder) {
        stringBuilder.append("\n");
    }

    private class RandomTestSuiteSample {

        private final Set<Integer> testIds;
        private final int testSuitId;
        private boolean isBugKilled;
        private final Map<String, List<Integer>> toolMutantResults;

        private long seed() {
            return testSuitId * 1000L + testSuitId;
        }

        RandomTestSuiteSample(int testSuitId, Random random, int numberOfTests, List<String> toolKeys) {
            this.testSuitId = testSuitId;
            testIds = new HashSet<>();
            while (testIds.size() < numberOfTests) {
                testIds.add(random.nextInt(allTestSuiteCount));
            }
            toolMutantResults = new HashMap<>();
            for (String toolKey : toolKeys) {
                toolMutantResults.put(toolKey, new ArrayList<>());
            }
        }

        public void addMutantResults(Map<String, Collection<String>> brokenTestsByMutant) {
            for (String o : brokenTestsByMutant.keySet()) {
                toolMutantResults.get(o).add(isKilled(brokenTestsByMutant.get(o)) ? 1 : 0);
            }
        }

        public boolean isKilled(Collection<String> brokenTestByBug) {
            boolean result = false;
            if (brokenTestByBug != null) {
                for (String s : brokenTestByBug) {
                    if (isTestInSample(getTestId(s))) {
                        result = true;
                        break;
                    }
                }
            }
            return result;
        }

        public boolean setIsBugKilled(Set<String> brokenTestByBug) {
            boolean result = false;
            assert brokenTestByBug != null && !brokenTestByBug.isEmpty() : "brokenTestByBug null or empty";
            for (String s : brokenTestByBug) {
                result |= isTestInSample(getTestId(s));
            }
            this.isBugKilled = result;
            return this.isBugKilled;
        }

        public boolean isTestInSample(int testId) {
            return testIds.contains(testId);
        }

        private List<Map<String, String>> getIbirCsvLines(String toolKey, List<Integer> mutantsNumbers) {
            List<Map<String, String>> result = new ArrayList<>();
            for (Integer mutantsNumber : mutantsNumbers) {
                if (mutantsNumber < 1) {
                    log.info("you passed a weird number of mutants = " + mutantsNumber);
                    continue;
                }
                if (mutantsNumber <= toolMutantResults.get(toolKey).size()) {
                    result.add(getIbirCsvLine(toolKey, mutantsNumber));
                } else {
                    log.info("we don't have that many mutants generated: you requested = " + mutantsNumber);
                }
            }
            return result;
        }

        private Map<String, String> getIbirCsvLine(String toolKey, int mutantsNumber) {
            Map<String, String> res = new HashMap<>();
            res.put(BUG_ID_COLUMN, projectBugId);
            res.put(TS_ID_COLUMN, String.valueOf(testSuitId));
            res.put(IS_BUG_KILLED_COLUMN, isBugKilled ? "1" : "0");
            int mutantsKilled = countIbirMutantsKilled(toolKey, mutantsNumber);
            res.put(KILLED_MUTANTS_COUNT_COLUMN, String.valueOf(mutantsKilled));
            res.put(MUTANTS_NUMBER_COLUMN, String.valueOf(mutantsNumber));
            res.put(KILLED_MUTANTS_PERCENT_COLUMN, String.valueOf(((float) mutantsKilled / (float) mutantsNumber)));
            return res;
        }

        private int countIbirMutantsKilled(String toolKey, int mutantsNumbers) {
            int res = 0;
            for (int i = 0; i < mutantsNumbers; i++) {
                res += toolMutantResults.get(toolKey).get(i);
            }
            return res;
        }

    }

    private static class ResultsHolder {
        private final List<Map<String, String>> tssCorrelationCsv = new ArrayList<>();
        private final List<Map<String, String>> tssOriginalBugFoundCorrelationCsv = new ArrayList<>();
        private final List<Map<String, String>> tssOriginalBugNotFoundCorrelationCsv = new ArrayList<>();

        public List<Map<String, String>> getTssCorrelationCsv() {
            return tssCorrelationCsv;
        }

        public List<Map<String, String>> getTssOriginalBugFoundCorrelationCsv() {
            return tssOriginalBugFoundCorrelationCsv;
        }

        public List<Map<String, String>> getTssOriginalBugNotFoundCorrelationCsv() {
            return tssOriginalBugNotFoundCorrelationCsv;
        }
    }
}
