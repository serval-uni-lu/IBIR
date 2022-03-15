package edu.lu.uni.serval.ibir;

import com.opencsv.exceptions.CsvValidationException;
import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.ibir.config.Configuration;
import edu.lu.uni.serval.ibir.info.IBIrPatch;
import edu.lu.uni.serval.ibir.localisation.*;
import edu.lu.uni.serval.ibir.output.*;
import edu.lu.uni.serval.ibir.patterns.*;
import edu.lu.uni.serval.ibir.utils.*;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.context.ContextReader;

import edu.lu.uni.serval.tbar.fixpatterns.*;
import edu.lu.uni.serval.tbar.fixpattern.FixTemplate;
import edu.lu.uni.serval.tbar.info.Patch;
import edu.lu.uni.serval.tbar.utils.Checker;
import edu.lu.uni.serval.tbar.utils.FileHelper;
import edu.lu.uni.serval.tbar.utils.SuspiciousCodeParser;
import edu.lu.uni.serval.tbar.utils.SuspiciousPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.UnknownServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static edu.lu.uni.serval.ibir.IbirInjector.LocalisationSelection.IBIR;
import static edu.lu.uni.serval.ibir.main.Main.D4J_INFOS_DIR;
import static edu.lu.uni.serval.ibir.utils.PerfectClassLocalisationUtils.loadPerfectLocalisationFiles;


/**
 * @see edu.lu.uni.serval.tbar.AbstractFixer
 * @see edu.lu.uni.serval.tbar.TBarFixer
 */
public class IbirInjector {

    private static final boolean PRINT_PATCHES = Boolean.getBoolean("PRINT_PATCHES");
    /**
     * keeps track of the progress and prints the results during exec.
     */
    private final ProgressPrinter progressPrinter;
    private final NonCompilePatches nonCompilePatchesCollector;

    public enum LocalisationSelection {IBIR, RANDOM, ALL}

    public enum Patterns {IBIR, RANDOM}

    public static ResultType resultType = ResultType.forId(System.getProperty("resultType"));
    public static final String BUG_LOCALISATION_FILE = System.getProperty("BUG_LOCALISATION_FILE", null);

    public static LocalisationSelection NODE_SELECTION = IBIR;
    public static Patterns PATTERNS = Patterns.IBIR;
    private static Logger log = LoggerFactory.getLogger(IbirInjector.class);

    protected String path = "";
    protected String projectName_bugId = "";     // The buggy project name.
    protected String defects4jPath;         // The path of local installed defects4j.
    private Integer failingTestsCount = null;                // Number of failed test cases before fixing.
    protected final String fullBuggyProjectPath;  // The full path of the local buggy project.
    public File suspCodePosFile = null;     // The file containing suspicious code positions localized by FL tools.
    protected DataPreparer dp;              // The needed data of buggy program for compiling and testing.

    // All specific failed test cases after testing the buggy project with defects4j command in Java code before fixing.
    protected List<String> failedTestCases = new ArrayList<>();

    // 0: failed to fix the bug, 1: succeeded to fix the bug. 2: partially succeeded to fix the bug.
    protected int patchId = 0;

    private String projectPath;

    /**
     * Used only for random generation.
     */
    private Random random;

    /**
     * They print out the results into matrices.
     */
    private final List<ResultCollector> resultCollectors;

    /**
     * Time since this injector instance was created.
     */
    private final Date startTime = new Date();


    // todo refactor
    /**
     * To avoid vrey long execution in the case of random location selection:
     * Once we have tried a certain number of patches and we didn't achieve the requested number of patches,
     * we start testing (not skipping) all patches.
     *
     * @see RandomSkipPatches#STOP_SKIPPING
     */
    private RandomSkipPatches randomSkipPatches;


    public IbirInjector(String path, final String projectName, int bugId, String defects4jPath, int numberOfMutants, Set<String> testsBrokenByOriginalBug) throws ExecutionException, InterruptedException, CsvValidationException, IOException {
        this.path = path;
        this.projectName_bugId = projectName + "_" + bugId;
        fullBuggyProjectPath = path + projectName_bugId;
        this.defects4jPath = defects4jPath;
        // Read paths of the buggy project.
        this.dp = new DataPreparer(path);
        projectPath = path + this.projectName_bugId;
        dp.prepareData(projectName_bugId, defects4jPath, projectPath);

        this.resultCollectors = new ArrayList<>();

        boolean exhaustiveInjection = LocalisationSelection.ALL.equals(NODE_SELECTION);
        if (ResultType.PCL_AND_DEFAULT.equals(resultType)) {
            Set<String> pclFiles = loadPerfectLocalisationFiles(D4J_INFOS_DIR, projectName, String.valueOf(bugId), dp.srcPath);
            this.resultCollectors.add(new PclResultCollector(numberOfMutants, pclFiles, testsBrokenByOriginalBug, projectName_bugId, exhaustiveInjection));
            this.resultCollectors.add(new ResultCollector(numberOfMutants, testsBrokenByOriginalBug, projectName_bugId, exhaustiveInjection));
        } else if (ResultType.PCL.equals(resultType)) {
            Set<String> pclFiles = loadPerfectLocalisationFiles(D4J_INFOS_DIR, projectName, String.valueOf(bugId), dp.srcPath);
            this.resultCollectors.add(new PclResultCollector(numberOfMutants, pclFiles, testsBrokenByOriginalBug, projectName_bugId, exhaustiveInjection));
        } else {
            this.resultCollectors.add(new ResultCollector(numberOfMutants, testsBrokenByOriginalBug, projectName_bugId, exhaustiveInjection));
        }


        final Path progressPrinterFile;
        if (NonCompilePatches.COMPILATION_CHECK) {
            nonCompilePatchesCollector = new NonCompilePatches(projectName_bugId);
            progressPrinterFile = nonCompilePatchesCollector.getDiscardedFilePath();
        } else {
            for (ResultCollector resultCollector : resultCollectors) {
                resultCollector.init();
            }
            nonCompilePatchesCollector = null;
            progressPrinterFile = Paths.get(Configuration.outputPath, String.valueOf(resultType), projectName_bugId + "_progress.csv");
        }

        progressPrinter = new ProgressPrinter(progressPrinterFile.toAbsolutePath().toString());
        progressPrinter.init();
        if (NonCompilePatches.COMPILATION_CHECK) {
            nonCompilePatchesCollector.loadAllDiscardedPatches(progressPrinter);
        }

        if (NODE_SELECTION.equals(LocalisationSelection.RANDOM)) {
            randomSkipPatches = new RandomSkipPatches(numberOfMutants, getRandom());
        }

    }

    public void countFailingTests() {
        failingTestsCount = TestUtils.testAllTestCases(fullBuggyProjectPath, defects4jPath, failedTestCases);
        log.warn("failing test counts = " + failingTestsCount);
    }

    public boolean hasFailingTests() {
        if (failingTestsCount == null) {
            countFailingTests();
        }
        return failingTestsCount > 0;
    }

    public void readTestCases() throws IOException {
        TestUtils.readTestCases(fullBuggyProjectPath, defects4jPath);
    }

    public List<SuspCodeNode> parseSuspiciousCode(SuspiciousPosition suspiciousCode) throws FileNotFoundException {
        String suspiciousJavaFile = suspiciousCode.classPath;
        int buggyLine = suspiciousCode.lineNumber;
        log.trace("suspicious code: " + suspiciousJavaFile + " ===" + buggyLine);

        String filePath = projectPath + File.separator + suspiciousJavaFile;
        File suspCodeFile = new File(filePath);
        if (!suspCodeFile.exists()) throw new FileNotFoundException("file = " + filePath);
        SuspiciousCodeParser scp = new SuspiciousCodeParser();
        scp.parseSuspiciousCode(new File(filePath), buggyLine);

        List<Pair<ITree, String>> suspiciousCodePairs = scp.getSuspiciousCode();
        if (suspiciousCodePairs.isEmpty()) {
            log.info("Failed to identify the buggy statement in: " + suspiciousJavaFile + " --- " + buggyLine);
            return null;
        }

        File targetJavaFile = suspCodeFile;
        String targetClassFilePath = dp.classPath + suspiciousJavaFile.substring(PathUtils.getSrcPath(this.projectName_bugId, defects4jPath, projectPath).get(2).length() - 1).trim().replace(".java", ".class");
        File targetClassFile = new File(targetClassFilePath);
        File javaBackup = new File(FileUtils.tempJavaPath(suspiciousJavaFile, Configuration.DATA_TYPE + "/" + this.projectName_bugId));
        File classBackup = new File(FileUtils.tempClassPath(suspiciousJavaFile, Configuration.DATA_TYPE + "/" + this.projectName_bugId));
        try {
            if (!targetClassFile.exists()) {
                int compilationResult = TestUtils.compileProjectWithDefects4j(fullBuggyProjectPath, defects4jPath);
                if (compilationResult == 1 || !targetClassFile.exists()) {
                    new FileNotFoundException("file = " + targetClassFile.getPath()).printStackTrace();
                    return null;
                }
            }
            if (javaBackup.exists()) javaBackup.delete();
            if (classBackup.exists()) classBackup.delete();
            Files.copy(targetJavaFile.toPath(), javaBackup.toPath());
            Files.copy(targetClassFile.toPath(), classBackup.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<SuspCodeNode> scns = new ArrayList<>();
        for (Pair<ITree, String> suspCodePair : suspiciousCodePairs) {
            ITree suspCodeAstNode = suspCodePair.getFirst(); //scp.getSuspiciousCodeAstNode();
            String suspCodeStr = suspCodePair.getSecond(); //scp.getSuspiciousCodeStr();
            log.debug("Suspicious Code: \n" + suspCodeStr);

            int startPos = suspCodeAstNode.getPos();
            int[] buggyLines = {buggyLine, buggyLine};
            SuspCodeNode scn = new SuspCodeNode(javaBackup, classBackup, targetJavaFile, targetClassFile,
                    startPos,startPos + suspCodeAstNode.getLength(), suspCodeAstNode, suspCodeStr, suspiciousJavaFile, buggyLines); // fixme store start and end lines
            scns.add(scn);
        }
        return scns;
    }

    private boolean isBudgetConsumed() {
        boolean result = true;
        for (ResultCollector resultCollector : resultCollectors) {
            result &= resultCollector.isBudgetConsumed();
        }
        return result;
    }

    private void testGeneratedPatches(List<Patch> patchCandidates, SuspCodeNode scn, FixTemplate ft, IbirSuspiciousPosition prioSuspeciousPosition) throws IOException, CsvValidationException {
        // Testing generated patches.
        for (Patch patch : patchCandidates) {
            // todo refactor this mess - separate or merge...
            if (NonCompilePatches.COMPILATION_CHECK && nonCompilePatchesCollector.isDone()) {
                nonCompilePatchesCollector.printCsvs();
                log.info("deleting temporary files...");
                FileHelper.deleteDirectory(Configuration.TEMP_FILES_PATH + Configuration.DATA_TYPE + "/" + this.projectName_bugId);
                log.info("done.");
                System.exit(0);
                return;
            }
            if (isBudgetConsumed()) return;
            if (shouldSkip(prioSuspeciousPosition.classPath)) break;
            // the patch object is being filled here.
            patch.buggyFileName = scn.suspiciousJavaFile;
            addPatchCodeToFile(scn, patch);// apply the patch and create the patch object fully.
            patchId++;
            // todo refactor this mess - separate or merge...
            // strange execution just to check if discarded patches are compilable or not.
            if (NonCompilePatches.COMPILATION_CHECK) {
                PatchEntry pe = nonCompilePatchesCollector.findPatchEntry(patch);
                if (pe != null) {
                    nonCompilePatchesCollector.removeFromInput(pe);
                    String buggyCode = patch.getBuggyCodeStr();

                    if (checkForStringIndexOutOfBoundsException(buggyCode)) {
                        continue; // ignore StringIndexOutOfBoundsException ones.
                    }


                    scn.targetClassFile.delete();

                    if (!isPatchedCodeCompilable(scn)) {
                        nonCompilePatchesCollector.addPatch(
                                String.valueOf(prioSuspeciousPosition.getLocalisationResultLine()),
                                String.valueOf(patchId), patch, getPatternName(ft),
                                String.valueOf(prioSuspeciousPosition.getLineStart()),
                                String.valueOf(prioSuspeciousPosition.getLineEnd()));
                    }
                }
            } else {
                // normal execution.
                PatchEntry duplicatePatchEntry = progressPrinter.addPatch(prioSuspeciousPosition.lineNumbers,
                prioSuspeciousPosition.getLocalisationResultLine(), patchId, patch, elapsedTimeMillis());
                if (duplicatePatchEntry != null) {

                    Map<ResultCollector, PatchEntry> containingRC = new HashMap();
                    for (ResultCollector resultCollector : resultCollectors) {
                        PatchEntry compilableDupl = resultCollector.exists(duplicatePatchEntry);
                        if (compilableDupl != null) {
                            resultCollector.printDuplicateToLocations(prioSuspeciousPosition.getLocalisationResultLine(), compilableDupl, prioSuspeciousPosition.getLineStart(), prioSuspeciousPosition.getLineEnd(), prioSuspeciousPosition.classPath);
                            containingRC.put(resultCollector, compilableDupl);
                        }
                    }
                    if (!containingRC.keySet().isEmpty() && containingRC.keySet().size() != resultCollectors.size()) {
                        Map<String, String> csvLine = null;
                        for (ResultCollector resultCollector : resultCollectors) {
                            if (!containingRC.containsKey(resultCollector)) {
                                if (resultCollector instanceof PclResultCollector) {
                                    if (((PclResultCollector) resultCollector).isPcl(prioSuspeciousPosition.classPath)) {
                                        if (csvLine == null) {
                                            ResultCollector rc = containingRC.keySet().iterator().next();
                                            csvLine = rc.getCsvLine(containingRC.get(rc));
                                        }
                                        resultCollector.addPatch(prioSuspeciousPosition.getLocalisationResultLine(), patchId, patch, csvLine, prioSuspeciousPosition.getLocalisationResultLineConfidence(), prioSuspeciousPosition.getLineStart(), prioSuspeciousPosition.getLineEnd());
                                    }
                                } else {
                                    if (csvLine == null) {
                                        ResultCollector rc = containingRC.keySet().iterator().next();
                                        csvLine = rc.getCsvLine(containingRC.get(rc));
                                    }
                                    resultCollector.addPatch(prioSuspeciousPosition.getLocalisationResultLine(), patchId, patch, csvLine, prioSuspeciousPosition.getLocalisationResultLineConfidence(), prioSuspeciousPosition.getLineStart(), prioSuspeciousPosition.getLineEnd());
                                }
                            }

                        }
                    }
                    continue;
                }

                String buggyCode = patch.getBuggyCodeStr();

                if (checkForStringIndexOutOfBoundsException(buggyCode))
                    continue; // ignore StringIndexOutOfBoundsException ones.

                String patchCode = patch.getFixedCodeStr1();
                scn.targetClassFile.delete();

                if (!isPatchedCodeCompilable(scn))
                    continue; // ignore not compilable patches

                log.info("--> running test suite for patchId: " + patchId);

                Set<String> failedTestsAfterFix;

                try {
                    failedTestsAfterFix = TestUtils.testAllTestCases(fullBuggyProjectPath, this.defects4jPath);
                } catch (UnknownServiceException e) {
                    log.error("couldn't run tests ! ");
                    continue; // ignore patches causing tests running errors.
                }
                int errorTestAfterFix = failedTestsAfterFix.size();


                MutantPatch mutantPatch = new MutantPatch(projectName_bugId, failedTestsAfterFix, errorTestAfterFix, patch, patchCode, getPatternName(ft), patchId, prioSuspeciousPosition);

                for (ResultCollector resultCollector : resultCollectors) {
                    resultCollector.addPatch(mutantPatch, elapsedTimeMillis());
                }

                if (PRINT_PATCHES) {
                    String patchStr = TestUtils.readPatch(this.fullBuggyProjectPath);
                    String patchFilePath = Configuration.outputPath + "_injectedBugs_patches/" + projectName_bugId + "/" + prioSuspeciousPosition.getLocalisationResultLine() + "/" + getPatternName(ft) + "/Patch_" + patchId + ".txt";

                    if (patchStr == null || !patchStr.trim().startsWith("diff")) {
                        FileHelper.outputToFile(patchFilePath,
                                "//**********************************************************\n//" + scn.suspiciousJavaFile
                                        + " ------ [" + scn.buggyLines[0]+" , "+scn.buggyLines[1]+"]"
                                        + "\n//**********************************************************\n"
                                        + "===Buggy Code===\n" + buggyCode + "\n\n===Patch Code===\n" + patchCode, false);
                    } else {
                        FileHelper.outputToFile(patchFilePath, patchStr + "\n", false);
                    }
                }
            }
        }
        resetScn(scn);
    }

    private String getPatternName(FixTemplate ft) {
        if (ft instanceof NamedPatterns) return ((NamedPatterns) ft).getName();
        return ft.getClass().getSimpleName();
    }

    private void resetScn(SuspCodeNode scn) {
        try {
            if (scn.targetJavaFile.exists())
                scn.targetJavaFile.delete();
            if (scn.targetClassFile.exists())
                scn.targetClassFile.delete();
            Files.copy(scn.javaBackup.toPath(), scn.targetJavaFile.toPath());
            Files.copy(scn.classBackup.toPath(), scn.targetClassFile.toPath());
            if (!scn.targetClassFile.exists()) {
                TestUtils.compileProjectWithDefects4j(fullBuggyProjectPath, defects4jPath);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private boolean isPatchedCodeCompilable(SuspCodeNode scn) {
        log.debug("Compiling patchId = " + patchId);
        try {// Compile patched file.
            String compilationResult = ShellUtils.shellRun(Arrays.asList("javac -Xlint:unchecked -source 1.7 -target 1.7 -cp "
                    + PathUtils.buildCompileClassPath(Arrays.asList(PathUtils.getJunitPath()), dp.classPath, dp.testClassPath)
                    + " -d " + dp.classPath + " " + scn.targetJavaFile.getAbsolutePath()), projectName_bugId, Configuration.SHELL_RUN_TIMEOUT_SEC);
            log.debug("isPatchedCodeCompilable: compilationResult = " + compilationResult);
        } catch (IOException e) {
            log.debug(projectName_bugId + " ---Fixer: fix fail because of javac exception! ");
            return false;
        }
        if (!scn.targetClassFile.exists()) { // fail to compile
            int results = TestUtils.compileProjectWithDefects4j(fullBuggyProjectPath, defects4jPath);
            if (results == 1) {
                log.debug(projectName_bugId + " ---Fixer: fix fail because of failed compiling! ");
                return false;
            }
        }
        log.debug("Finish of compiling.");
        return true;
    }

    private boolean checkForStringIndexOutOfBoundsException(String buggyCode) {
        return "===StringIndexOutOfBoundsException===".equals(buggyCode);
    }

    private void addPatchCodeToFile(SuspCodeNode scn, Patch patch) {
        try {
            GitUtils.checkoutProjectRepoHead(fullBuggyProjectPath);
        } catch (IOException e) {
            log.error("error in checking the head", e);
        }
        String javaCode = FileHelper.readFile(scn.javaBackup);

        String fixedCodeStr1 = patch.getFixedCodeStr1();
        String fixedCodeStr2 = patch.getFixedCodeStr2();
        int exactBuggyCodeStartPos = patch.getBuggyCodeStartPos();
        int exactBuggyCodeEndPos = patch.getBuggyCodeEndPos();
        String patchCode = fixedCodeStr1;
        boolean needBuggyCode = false;
        if (exactBuggyCodeEndPos > exactBuggyCodeStartPos) {
            if ("MOVE-BUGGY-STATEMENT".equals(fixedCodeStr2)) {
                // move statement position.
            } else if (exactBuggyCodeStartPos != -1 && exactBuggyCodeStartPos < scn.startPos) {
                // Remove the buggy method declaration.
            } else {
                needBuggyCode = !(patch instanceof IBIrPatch) || ((IBIrPatch) patch).getNeedBugyCode(); // this is always true for the reused patterns.
                if (exactBuggyCodeStartPos == 0) {
                    // Insert the missing override method, the buggy node is TypeDeclaration.
                    int pos = scn.suspCodeAstNode.getPos() + scn.suspCodeAstNode.getLength() - 1;
                    for (int i = pos; i >= 0; i--) {
                        if (javaCode.charAt(i) == '}') {
                            exactBuggyCodeStartPos = i;
                            exactBuggyCodeEndPos = i + 1;
                            break;
                        }
                    }
                } else if (exactBuggyCodeStartPos == -1) {
                    // Insert generated patch code before the buggy code.
                    exactBuggyCodeStartPos = scn.startPos;
                    exactBuggyCodeEndPos = scn.endPos;
                } else {
                    // Insert a block-held statement to surround the buggy code
                }
            }
        } else if (exactBuggyCodeStartPos == -1 && exactBuggyCodeEndPos == -1) {
            // Replace the buggy code with the generated patch code.
            exactBuggyCodeStartPos = scn.startPos;
            exactBuggyCodeEndPos = scn.endPos;
        } else if (exactBuggyCodeStartPos == exactBuggyCodeEndPos) {
            // Remove buggy variable declaration statement.
            exactBuggyCodeStartPos = scn.startPos;
        }

        patch.setBuggyCodeStartPos(exactBuggyCodeStartPos);
        patch.setBuggyCodeEndPos(exactBuggyCodeEndPos);
        String buggyCode;
        try {
            buggyCode = javaCode.substring(exactBuggyCodeStartPos, exactBuggyCodeEndPos);
            if (needBuggyCode) {
                patchCode += buggyCode;
                if (fixedCodeStr2 != null) {
                    patchCode += fixedCodeStr2;
                }
            }

            File newFile = new File(scn.targetJavaFile.getAbsolutePath() + ".temp");
            String patchedJavaFile = javaCode.substring(0, exactBuggyCodeStartPos) + patchCode + javaCode.substring(exactBuggyCodeEndPos);
            FileHelper.outputToFile(newFile, patchedJavaFile, false);
            newFile.renameTo(scn.targetJavaFile);
        } catch (StringIndexOutOfBoundsException e) {
            log.debug(exactBuggyCodeStartPos + " ==> " + exactBuggyCodeEndPos + " : " + javaCode.length());
            e.printStackTrace();
            buggyCode = "===StringIndexOutOfBoundsException===";
        }

        patch.setBuggyCodeStr(buggyCode);
        patch.setFixedCodeStr1(patchCode);
    }


    private Random getRandom() {
        assert LocalisationSelection.RANDOM.equals(NODE_SELECTION);
        if (random == null) {
            random = new Random();
        }
        return random;
    }

    private void injectExhaustively() throws InjectionAbortingException, CsvValidationException, IOException {
        // Read paths of the project.
        if (!dp.validPaths)
            throw new InjectionAbortingException("dp.validPaths is false.");

        log.info("======= EXHAUSTIVE INJECTOR: Start to inject a bug in the target code ======");

        final AllFileLocalisationProvider localisationProvider;

        try {
            localisationProvider = AllFileLocalisationProvider.newInstance(dp.srcAbsolutePath, projectName_bugId, projectPath, resultType, dp.classPath, defects4jPath);
        } catch (CsvValidationException | IOException e) {
            throw new InjectionAbortingException(e);
        }

        while (!isBudgetConsumed()) {
            SuspCodeNode scn = localisationProvider.getNextSuspCodeNode((throwable, file, allJavaFiles) -> {
                if (throwable instanceof AllFileLocalisationProvider.MissingBinary) {
                    // recompile
                    int compilationResult = TestUtils.compileProjectWithDefects4j(fullBuggyProjectPath, defects4jPath);
                    if (compilationResult != 1 && throwable.file.exists()) {
                        return false;
                    }
                } else {
                    allJavaFiles.remove(file);
                }
                return true;
            });

            if (scn == null) {
                assert !localisationProvider.hasNodes() : "localisationProvider still has nodes and returned null";
                break;
            }

            log.debug("scn = \n" + scn.suspCodeStr);

            // Parse context information of the suspicious code.
            List<Integer> contextInfoList = readAllNodeTypes(scn.suspCodeAstNode);
            List<Integer> distinctContextInfo = new ArrayList<>();
            for (Integer contInfo : contextInfoList) {
                if (!distinctContextInfo.contains(contInfo) && !Checker.isBlock(contInfo)) {
                    distinctContextInfo.add(contInfo);
                }
            }
            // Match fix templates for this suspicious code with its context information.
            int[] positionInSourceCodeFile = {scn.startPos, scn.endPos};
            injectWithMatchedTemplates(scn, distinctContextInfo, new IbirSuspiciousPosition(scn.buggyLines,scn.suspiciousJavaFile, positionInSourceCodeFile));
        }
        log.info("=======INJECTOR : Finish off injecting======");
        log.info("deleting temporary files...");
        FileHelper.deleteDirectory(Configuration.TEMP_FILES_PATH + Configuration.DATA_TYPE + "/" + this.projectName_bugId);
        log.info("done.");
    }

    public void injectProcessRandomly() throws InjectionAbortingException, IOException, CsvValidationException {
        // Read paths of the buggy project.
        if (!dp.validPaths)
            throw new InjectionAbortingException("dp.validPaths is false.");

        log.info("======= RANDOM INJECTOR: Start to inject a bug in the target code ======");

        assert randomSkipPatches != null : "randomSkipPatches null";

        final RandomLocalisationProvider localisationProvider;

        try {
            localisationProvider = RandomLocalisationProvider.newInstance(dp.srcAbsolutePath, projectName_bugId, projectPath, resultType, dp.classPath, defects4jPath);
        } catch (CsvValidationException | IOException e) {
            throw new InjectionAbortingException(e);
        }

        while (!isBudgetConsumed()) {
            SuspCodeNode scn = localisationProvider.getSuspCodeNode((throwable, file, allJavaFiles) -> {
                if (throwable instanceof RandomLocalisationProvider.MissingBinary) {
                    // recompile
                    int compilationResult = TestUtils.compileProjectWithDefects4j(fullBuggyProjectPath, defects4jPath);
                    if (compilationResult != 1 && throwable.file.exists()) {
                        return false;
                    }
                } else {
                    allJavaFiles.remove(file);
                }
                return true;
            }, new RandomLocalisationProvider.ExcludeJavaFilter() {
                @Override
                public Set<String> includeOnly() {
                    Set<String> result = new HashSet<>();
                    for (ResultCollector resultCollector : resultCollectors) {
                        if (resultCollector instanceof PclResultCollector) {
                            result.addAll(resultCollector.getPclFiles());
                        }
                    }
                    return result;
                }

                @Override
                public boolean shouldSkip(String javaFile) {
                    return IbirInjector.this.shouldSkip(javaFile);
                }
            });

            if (scn == null) {
                assert !localisationProvider.hasNodes() : "localisationProvider still has nodes and returned null";
                execAllSkippedPatches();
                break;
            }

            log.debug("scn = \n" + scn.suspCodeStr);

            // Parse context information of the suspicious code.
            List<Integer> contextInfoList = readAllNodeTypes(scn.suspCodeAstNode);
            List<Integer> distinctContextInfo = new ArrayList<>();
            for (Integer contInfo : contextInfoList) {
                if (!distinctContextInfo.contains(contInfo) && !Checker.isBlock(contInfo)) {
                    distinctContextInfo.add(contInfo);
                }
            }
            // Match fix templates for this suspicious code with its context information.
            int[] scnPos = {scn.startPos, scn.endPos};
            injectWithMatchedTemplates(scn, distinctContextInfo, new IbirSuspiciousPosition(scn.buggyLines,scn.suspiciousJavaFile, scnPos));
        }
        log.info("=======INJECTOR : Finish off injecting======");
        log.info("deleting temporary files...");
        FileHelper.deleteDirectory(Configuration.TEMP_FILES_PATH + Configuration.DATA_TYPE + "/" + this.projectName_bugId);
        log.info("done.");
    }

    public void injectProcess() throws InjectionAbortingException, IOException, CsvValidationException {


        if (LocalisationSelection.RANDOM.equals(NODE_SELECTION)) {
            injectProcessRandomly();
            return;
        } else if (LocalisationSelection.ALL.equals(NODE_SELECTION)){
            injectExhaustively();
            return;
        }

        // Read paths of the buggy project.
        if (!dp.validPaths)
            throw new InjectionAbortingException("dp.validPaths is false.");

        // Read suspicious positions.
        List<PrioSuspeciousPosition> suspiciousCodeList;
        try {
            suspiciousCodeList = readSuspiciousCodeFromFile();
        } catch (LocalisationFailedException e) {
            throw new InjectionAbortingException(e);
        }

        List<SuspCodeNode> triedSuspNode = new ArrayList<>();
        log.info("======= INJECTOR: Start to inject a bug in the target code ======");
        for (PrioSuspeciousPosition suspiciousCode : suspiciousCodeList) {
            log.info("--- suspicious loc = " + suspiciousCode.localisationResultLine);
            if (shouldSkip(suspiciousCode.classPath)) {
                log.info("SKIP: skipped class = " + suspiciousCode.classPath);
                continue;
            }
            List<SuspCodeNode> scns;
            try {
                scns = parseSuspiciousCode(suspiciousCode);
            } catch (FileNotFoundException e) {
                log.error("SKIP: null or empty scns", e);
                continue;
            }
            if (scns == null || scns.isEmpty()) {
                log.info("SKIP: null or empty scns");
                continue;
            }

            for (SuspCodeNode scn : scns) {
                log.debug("scn = \n" + scn.suspCodeStr);
                if (triedSuspNode.contains(scn)) {
                    log.info("SKIP: already tried scn");
                    continue;
                }
                triedSuspNode.add(scn);

                // Parse context information of the suspicious code.
                List<Integer> contextInfoList = readAllNodeTypes(scn.suspCodeAstNode);
                List<Integer> distinctContextInfo = new ArrayList<>();
                for (Integer contInfo : contextInfoList) {
                    if (!distinctContextInfo.contains(contInfo) && !Checker.isBlock(contInfo)) {
                        distinctContextInfo.add(contInfo);
                    }
                }

                // Match fix templates for this suspicious code with its context information.
                injectWithMatchedTemplates(scn, distinctContextInfo, suspiciousCode);
                if (isBudgetConsumed()) break;
            }
            if (isBudgetConsumed()) break;
        }
        log.info("=======INJECTOR : Finish off injecting======");
        if (NonCompilePatches.COMPILATION_CHECK) {
            nonCompilePatchesCollector.printCsvs();
            System.exit(0);
            return;
        }
        log.info("deleting temporary files...");
        FileHelper.deleteDirectory(Configuration.TEMP_FILES_PATH + Configuration.DATA_TYPE + "/" + this.projectName_bugId);
        log.info("done.");
    }

    private boolean shouldSkip(String classPath) {
        boolean result = true;
        for (ResultCollector resultCollector : resultCollectors) {
            result &= resultCollector.shouldSkip(classPath);
        }
        return result;
    }

    public List<PrioSuspeciousPosition> readSuspiciousCodeFromFile() throws LocalisationFailedException {
        String suspiciousFilePath = "";
        if (this.suspCodePosFile == null) {
            suspiciousFilePath = Configuration.bugInjectionTargetFilePath;
        } else {
            suspiciousFilePath = this.suspCodePosFile.getPath();
        }
        log.info("------ reading localisation from : " + suspiciousFilePath);

        return IBIrLocalisationProvider.newInstance(suspiciousFilePath + "/" + (BUG_LOCALISATION_FILE == null ? this.projectName_bugId : BUG_LOCALISATION_FILE)).getSuspeciousCodePositions();
    }

    public void injectWithMatchedTemplates(SuspCodeNode scn, List<Integer> distinctContextInfo, IbirSuspiciousPosition prioSuspeciousPosition) throws IOException, CsvValidationException { //  here comes the magic.
        // generate patches with fix templates of TBar.
        FixTemplate ft = null;
        int suspCodeASTNodeType = scn.suspCodeAstNode.getType();

        if (!Checker.isMethodDeclaration(suspCodeASTNodeType)) {
            if (Checker.isExpressionStatement(suspCodeASTNodeType) ||
                    Checker.isIfStatement(suspCodeASTNodeType) ||
                    Checker.isReturnStatement(suspCodeASTNodeType) ||
                    Checker.isVariableDeclarationStatement(suspCodeASTNodeType) ||
                    Checker.isConstructorInvocation(suspCodeASTNodeType) ||
                    Checker.isSuperConstructorInvocation(suspCodeASTNodeType) ||
                    Checker.isSwitchCase(suspCodeASTNodeType)) {
                boolean nullChecked = false;
                boolean typeChanged = false;
                boolean methodChanged = false;
                boolean operator = false;

                for (Integer contextInfo : distinctContextInfo) {
                    if (Patterns.IBIR.equals(PATTERNS) && Checker.isCastExpression(contextInfo)) {
                        if (!typeChanged) {
                            typeChanged = true;
                            ft = new DataTypeReplacer();
                        }
                    } else if (Patterns.IBIR.equals(PATTERNS) && Checker.isClassInstanceCreation(contextInfo)) {
                        if (!methodChanged) {
                            methodChanged = true;
                            ft = new MethodInvocationMutator();
                        }
                    } else if (Checker.isIfStatement(contextInfo) || Checker.isDoStatement(contextInfo) || Checker.isWhileStatement(contextInfo)) {
                        if (Checker.isInfixExpression(scn.suspCodeAstNode.getChild(0).getType()) && !operator) {
                            operator = true;
                            ft = new OperatorMutator(0); // ALL
                            generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                            ft = null;
                            if (isBudgetConsumed()) return;
                        } else if (Patterns.RANDOM.equals(PATTERNS)) {
                            ft = new OperatorMutator(0);
                            operator = true;
                            generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                            ft = null;
                            if (isBudgetConsumed()) return;
                        }
                        if (Patterns.IBIR.equals(PATTERNS)) {
                            ft = new InstanceOfRemover();
                            generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                            if (isBudgetConsumed()) return;
                            ft = new NullPointerCheckerRemover();
                            generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                            if (isBudgetConsumed()) return;
                            ft = new RangeCheckerRemover();
                            generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                            if (isBudgetConsumed()) return;

                            ft = new IbirConditionalExpressionMutator(2);
                        }
                    } else if (Patterns.IBIR.equals(PATTERNS) && Checker.isConditionalExpression(contextInfo)) {
                        ft = new InstanceOfRemover();
                        generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                        if (isBudgetConsumed()) return;
                        ft = new NullPointerCheckerRemover();
                        generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                        if (isBudgetConsumed()) return;
                        ft = new RangeCheckerRemover();
                        generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                        if (isBudgetConsumed()) return;
                        ft = new IbirConditionalExpressionMutator(0);
                    } else if (Patterns.IBIR.equals(PATTERNS) && (Checker.isCatchClause(contextInfo) || Checker.isVariableDeclarationStatement(contextInfo))) {
                        if (!typeChanged) {
                            ft = new DataTypeReplacer();
                            typeChanged = true;
                        }
                    } else if (Checker.isInfixExpression(contextInfo)) {
                        if (Patterns.IBIR.equals(PATTERNS)) {
                            ft = new InjectCASTIdivCastToDouble();
                            generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                            if (isBudgetConsumed()) return;
                            ft = null;
                        }

                        if (!operator) {
                            operator = true;
                            ft = new OperatorMutator(0);
                            generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                            ft = null;
                            if (isBudgetConsumed()) return;
                        }
                        if (Patterns.IBIR.equals(PATTERNS)) {
                            ft = new IbirConditionalExpressionMutator(1);
                            generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                            if (isBudgetConsumed()) return;
                            ft = new InstanceOfRemover();
                            generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                            if (isBudgetConsumed()) return;
                            ft = new NullPointerCheckerRemover();
                            generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                            if (isBudgetConsumed()) return;
                            ft = new RangeCheckerRemover();
                            generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                            ft = null;
                            if (isBudgetConsumed()) return;
                        }

                        ft = new OperatorMutator(4);
                    } else if ((Patterns.IBIR.equals(PATTERNS)) && (Checker.isBooleanLiteral(contextInfo) || Checker.isNumberLiteral(contextInfo) || Checker.isCharacterLiteral(contextInfo) || Checker.isStringLiteral(contextInfo))) {
                        ft = new InstanceOfRemover();
                        generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                        if (isBudgetConsumed()) return;
                        ft = new NullPointerCheckerRemover();
                        generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                        if (isBudgetConsumed()) return;
                        ft = new RangeCheckerRemover();
                        generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                        if (isBudgetConsumed()) return;
                        ft = new LiteralExpressionMutator(); // todo should we add this to random also?
                    } else if (Checker.isMethodInvocation(contextInfo) || Checker.isConstructorInvocation(contextInfo) || Checker.isSuperConstructorInvocation(contextInfo)) {
                        if (Patterns.IBIR.equals(PATTERNS)) {
                            if (!methodChanged) {
                                ft = new MethodInvocationMutator();
                                methodChanged = true;
                            }

                            if (Checker.isMethodInvocation(contextInfo)) {
                                if (ft != null) {
                                    generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                                    if (isBudgetConsumed()) return;
                                }
                                ft = new NPEqualsShouldHandleNullArgument();
                                generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                                if (isBudgetConsumed()) return;
                            }
                        }
                    } else if (Patterns.IBIR.equals(PATTERNS) && Checker.isAssignment(contextInfo)) {
                        ft = new OperatorMutator(2); // todo keep ignoring this on random?
                    } else if (Patterns.IBIR.equals(PATTERNS) && Checker.isInstanceofExpression(contextInfo)) {
                        ft = new OperatorMutator(5);
                    } else if (Patterns.IBIR.equals(PATTERNS) && Checker.isReturnStatement(contextInfo)) {
                        String returnType = ContextReader.readMethodReturnType(scn.suspCodeAstNode);
                        if ("boolean".equalsIgnoreCase(returnType)) {
                            ft = new InstanceOfRemover();
                            generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                            if (isBudgetConsumed()) return;
                            ft = new NullPointerCheckerRemover();
                            generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                            if (isBudgetConsumed()) return;
                            ft = new RangeCheckerRemover();
                            generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                            if (isBudgetConsumed()) return;
                            ft = new IbirConditionalExpressionMutator(2);
                        } else {
                            ft = new ReturnStatementMutator(returnType);
                        }
                    } else if (Patterns.IBIR.equals(PATTERNS) && (Checker.isSimpleName(contextInfo) || Checker.isQualifiedName(contextInfo))) {
                        ft = new VariableReplacer();
                    }
                    if (ft != null) {
                        generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                        if (isBudgetConsumed()) return;
                    }
                    ft = null;
                }

                if (Patterns.IBIR.equals(PATTERNS)) {
                    ft = new StatementMover();
                    generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                    if (isBudgetConsumed()) return;
                }

                ft = new IBIrStatementRemover();
                ((IBIrStatementRemover) ft).setParentMethodRemovingAllowed(Patterns.IBIR.equals(PATTERNS));
                generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                if (isBudgetConsumed()) return;
                if (Patterns.IBIR.equals(PATTERNS)) {
                    ft = new StatementInserter();
                    generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
                    if (isBudgetConsumed()) return;
                }
            }
        } else {
            ft = new IBIrStatementRemover();
            ((IBIrStatementRemover) ft).setParentMethodRemovingAllowed(Patterns.IBIR.equals(PATTERNS));
            generateAndValidatePatches(ft, scn, prioSuspeciousPosition);
            if (isBudgetConsumed()) return;
        }
    }

    protected void generateAndValidatePatches(FixTemplate ft, SuspCodeNode scn, IbirSuspiciousPosition prioSuspeciousPosition) throws IOException, CsvValidationException {
        ft.setSuspiciousCodeStr(scn.suspCodeStr);
        ft.setSuspiciousCodeTree(scn.suspCodeAstNode);
        if (scn.javaBackup == null) ft.setSourceCodePath(dp.srcAbsolutePath);
        else ft.setSourceCodePath(dp.srcAbsolutePath, scn.javaBackup);
        ft.generatePatches();
        List<Patch> patchCandidates = ft.getPatches();
        // Test generated patches.
        if (patchCandidates.isEmpty()) return;

        List<Patch> patchesToTest = null;
        if (LocalisationSelection.RANDOM.equals(NODE_SELECTION)) { // TODO: 10/03/2022 needed for exhaustive.
            patchCandidates = progressPrinter.removeTriedOnes(patchCandidates); // TODO: 09/03/2022 how is this one working?
            if (patchCandidates.isEmpty()) return;
            if (!randomSkipPatches.stopSkipping(progressPrinter.triedPatchesCount())) {
                if (getRandom().nextInt(10) < 7) {
                    randomSkipPatches.addSkippedPatches(patchCandidates, scn, ft, prioSuspeciousPosition);
                    return; // 4/10 probability to execute a generated patch: reduce the chances that the mutants come from the same scns.
                }
                final Patch patch = RandomLocalisationProvider.randomItem(getRandom(), patchCandidates);
                // patchCandidates.remove(patch); fixme we can't use remove because of the wierd way patches are being created...
                randomSkipPatches.addSkippedPatches(patchCandidates, scn, ft, prioSuspeciousPosition);
                patchesToTest = new ArrayList<Patch>() {{
                    add(patch);
                }};
            } else {
                execAllSkippedPatches();
                patchesToTest = patchCandidates;
            }
        } else {
            patchesToTest = patchCandidates;
        }
        testGeneratedPatches(patchesToTest, scn, ft, prioSuspeciousPosition);
    }

    private void execAllSkippedPatches() throws CsvValidationException, IOException {
        List<RandomSkipPatches.SkippedPatch> skippedPatches = randomSkipPatches.pollShuffledSkippedPatches();
        Iterator<RandomSkipPatches.SkippedPatch> iter = skippedPatches.iterator();
        while (!isBudgetConsumed() && iter.hasNext()) {
            RandomSkipPatches.SkippedPatch p = iter.next();
            testGeneratedPatches(new ArrayList<Patch>() {{
                add(p.patch);
            }}, p.scn, p.ft, p.prioSuspeciousPosition);
        }
    }

    private long elapsedTimeMillis() {
        return new Date().getTime() - startTime.getTime();
    }

    private List<Integer> readAllNodeTypes(ITree suspCodeAstNode) {
        List<Integer> nodeTypes = new ArrayList<>();
        nodeTypes.add(suspCodeAstNode.getType());
        List<ITree> children = suspCodeAstNode.getChildren();
        for (ITree child : children) {
            int childType = child.getType();
            if (Checker.isFieldDeclaration(childType) ||
                    Checker.isMethodDeclaration(childType) ||
                    Checker.isTypeDeclaration(childType) ||
                    Checker.isStatement(childType)) break;
            nodeTypes.addAll(readAllNodeTypes(child));
        }
        return nodeTypes;
    }

}
