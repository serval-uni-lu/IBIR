package edu.lu.uni.serval.ibir.localisation;

import com.opencsv.exceptions.CsvValidationException;
import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.ibir.output.ResultType;
import edu.lu.uni.serval.ibir.utils.FileUtils;
import edu.lu.uni.serval.ibir.utils.PathUtils;
import edu.lu.uni.serval.jdt.tree.ITree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static edu.lu.uni.serval.ibir.output.ResultType.PCL;
import static edu.lu.uni.serval.ibir.config.Configuration.DATA_TYPE;
import static edu.lu.uni.serval.ibir.main.Main.D4J_INFOS_DIR;
import static edu.lu.uni.serval.ibir.utils.DataPreparer.srcPath;
import static edu.lu.uni.serval.ibir.utils.PerfectClassLocalisationUtils.loadPerfectLocalisationFiles;


/**
 * //0,CLOSURE-253,src/com/google/javascript/jscomp/OptimizeReturns.java,0.23481676615063515,['44-44']
 * //7,CLOSURE-253,src/com/google/javascript/jscomp/Compiler.java,0.15470502888342288,"['588-588', '623-623']"
 */
public class RandomLocalisationProvider {

    enum LoadingMode{ SHUFFLE_ALL, ONE_BY_ONE}
    private static final LoadingMode LOADING_MODE = LoadingMode.SHUFFLE_ALL;

    private static final String JAVA_FILE_EXTENSION = ".java";
    private static Logger log = LoggerFactory.getLogger(RandomLocalisationProvider.class);
    private final String projectClassPath;
    private final String defects4jPath;

    /**
     * the path to the project + the path until the classes. i.e. path/to/Lang_1/src/main/java/
     */
    private Set<File> allJavaFiles;
    private final Map<File, List<Pair<ITree, AstParser.AstNode>>> fileAstList;

    /**
     * 2 = 1 line for header + 1 Line for a localisation entry.
     */
    private static final int MINIMUM_LOCALISATION_FILE_LENGTH = 2;
    private Random random;
    private final String buggyProject;
    private final String projectPath;


    public static RandomLocalisationProvider newInstance(String projectSrcPath, String buggyProject, String projectPath, ResultType resultType, String projectClassPath, String defects4jPath) throws CsvValidationException, IOException {
        if (PCL.equals(resultType)) {
            return newPclInstance(buggyProject, projectPath, projectClassPath, defects4jPath);
        } else {
            return newInstance(projectSrcPath, buggyProject, projectPath, projectClassPath, defects4jPath);
        }
    }

    private static RandomLocalisationProvider newInstance(String projectSrcPath, String buggyProject, String projectPath, String projectClassPath, String defects4jPath) {
        Set<File> allJavaFiles = getAllJavaFiles(projectSrcPath);
        assert allJavaFiles != null && !allJavaFiles.isEmpty();
        return new RandomLocalisationProvider(allJavaFiles, buggyProject, projectPath, projectClassPath, defects4jPath);
    }

    private static RandomLocalisationProvider newPclInstance(String buggyProject, String projectPath, String projectClassPath, String defects4jPath) throws CsvValidationException, IOException {
        Set<File> allJavaFiles = getPclFiles(projectPath, buggyProject);
        assert !allJavaFiles.isEmpty();
        return new RandomLocalisationProvider(allJavaFiles, buggyProject, projectPath, projectClassPath, defects4jPath);
    }

    private static Set<File> getPclFiles(String projectPath, String buggyProject) throws CsvValidationException, IOException {
        String[] splitted = buggyProject.split("_");
        Set<String> pclFileClassPaths = loadPerfectLocalisationFiles(D4J_INFOS_DIR, splitted[0], splitted[1], srcPath);
        Set<File> allJavaFiles = new HashSet<>();
        for (String pclFilePath : pclFileClassPaths) {
            File file = new File(projectPath + "/" + pclFilePath);
            assert file.exists();
            allJavaFiles.add(file);
        }
        return allJavaFiles;
    }

    private static Set<File> getAllJavaFiles(String projectSrcPath) {
        assert projectSrcPath != null && !projectSrcPath.isEmpty();
        File srcDir = new File(projectSrcPath);
        return getAllJavaFiles(srcDir);
    }

    private static Set<File> getAllJavaFiles(File srcDir) {
        assert srcDir != null;
        Set<File> result = new HashSet<>();
        for (final File fileEntry : srcDir.listFiles()) {
            if (fileEntry.isDirectory()) {
                result.addAll(getAllJavaFiles(fileEntry));
            } else if (fileEntry.isFile() && fileEntry.getPath().endsWith(JAVA_FILE_EXTENSION)) {
                result.add(fileEntry);
            }
        }
        return result;
    }

    private RandomLocalisationProvider(Set<File> allJavaFiles, String buggyProject, String projectPath, String projectClassPath, String defects4jPath) {
        this.allJavaFiles = allJavaFiles;
        this.buggyProject = buggyProject;
        this.projectPath = projectPath;
        this.fileAstList = new HashMap<>();
        this.projectClassPath = projectClassPath;
        this.defects4jPath = defects4jPath;
    }

    private List<Pair<Pair<ITree, AstParser.AstNode>, File>> nodeFilePair;

    public void init(ErrorCallback errorCallback) {
        // load all files AST in a list and shuffle them.
        Set<File> allFiles = new HashSet<>(allJavaFiles);
        nodeFilePair = new ArrayList<>();
        for (File javaFile : allFiles) {
            FileLoadingException e = null;
            if (!javaFile.exists()) {
                e = new FileLoadingException(javaFile);
                if (errorCallback == null || errorCallback.onError(e, javaFile, allJavaFiles)) continue;
            }
            List<Pair<ITree, AstParser.AstNode>> fAst = getFileInterestingAstNodes(javaFile);
            if (fAst == null || fAst.isEmpty()) {
                e = new FileLoadingException(javaFile);
                if (errorCallback == null || errorCallback.onError(e, javaFile, allJavaFiles)) continue;
            } else {
                for (Pair<ITree, AstParser.AstNode> nodeAst : fAst) {
                    nodeFilePair.add(new Pair<>(nodeAst, javaFile));
                }
            }
        }
        Collections.shuffle(nodeFilePair, getRandom());
    }

    private Random getRandom() {
        if (this.random == null) {
            this.random = new Random();
        }
        return this.random;
    }

    public static <T> T randomItem(Random sourceOfRandomness, List<T> list) {
        int length = list.size();
        return list.get(sourceOfRandomness.nextInt(length));
    }

    private List<Pair<ITree, AstParser.AstNode>> getFileInterestingAstNodes(File javaFile) {
        if (!fileAstList.containsKey(javaFile)) {
            List<Pair<ITree, AstParser.AstNode>> ast = new AstParser().parseSuspiciousCode(javaFile);
            fileAstList.put(javaFile, ast);
        }
        return fileAstList.get(javaFile);
    }

    // it has to be like /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
    private String getSrcJavaFilePath(File javaFile) {
        return javaFile.getPath().substring(projectPath.length());
    }

    private File getFileFromSrcJavaPath(String javaFilePath) {
        return Paths.get(projectPath, javaFilePath).toFile();
    }

    private void removeFileFromNodesList(File javaFile) {
        Set<Pair<Pair<ITree, AstParser.AstNode>, File>> nodeFilePairToDelete = new HashSet<>();
        for (Pair<Pair<ITree, AstParser.AstNode>, File> pairFilePair : nodeFilePair) {
            if (javaFile.equals(pairFilePair.secondElement)) nodeFilePairToDelete.add(pairFilePair);
        }
        nodeFilePair.removeAll(nodeFilePairToDelete);
    }

    public SuspCodeNode getNextSuspCodeNode(ErrorCallback errorCallback, ExcludeJavaFilter excludeScnFilter) {
        if (nodeFilePair == null) init(errorCallback);
        if (nodeFilePair.isEmpty()) return null;
        SuspCodeNode suspCodeNode = null;
        FileLoadingException e = null;
        while (suspCodeNode == null && !nodeFilePair.isEmpty()) {
            Pair<Pair<ITree, AstParser.AstNode>, File> pair = nodeFilePair.get(0);
            nodeFilePair.remove(0);
            File javaFile = pair.secondElement;
            Pair<ITree, AstParser.AstNode> suspCodePair = pair.firstElement;
            if (!javaFile.exists()) {
                e = new FileLoadingException(javaFile);
                if (errorCallback == null || errorCallback.onError(e, javaFile, allJavaFiles)) {
                    removeFileFromNodesList(javaFile);
                    continue;
                }
            }
            String javaFilePath = getSrcJavaFilePath(javaFile);
            if (excludeScnFilter != null && excludeScnFilter.shouldSkip(javaFilePath.substring(1))) {
                assert excludeScnFilter.includeOnly() != null && !excludeScnFilter.includeOnly().isEmpty();
                allJavaFiles = new HashSet<>();
                for (String s : excludeScnFilter.includeOnly()) {
                    File file = getFileFromSrcJavaPath(s);
                    assert file.exists();
                    allJavaFiles.add(file);
                }
                init(errorCallback);
                continue;
            }

            String targetClassFilePath = projectClassPath + javaFilePath.substring(PathUtils.getSrcPath(this.buggyProject, defects4jPath, projectPath).get(2).length()).trim().replace(".java", ".class");
            File targetClassFile = new File(targetClassFilePath);
            File javaBackup = new File(FileUtils.tempJavaPath(javaFilePath, DATA_TYPE + "/" + this.buggyProject));
            File classBackup = new File(FileUtils.tempClassPath(javaFilePath, DATA_TYPE + "/" + buggyProject));
            try {
                if (!targetClassFile.exists()) {
                    log.error("class not found = " + targetClassFile);
                    e = new MissingBinary(targetClassFile);
                    if (errorCallback == null || errorCallback.onError(e, javaFile, allJavaFiles)) {
                        removeFileFromNodesList(javaFile);
                        continue;
                    }
                }
                assert targetClassFile.exists();
                if (javaBackup.exists()) javaBackup.delete();
                if (classBackup.exists()) classBackup.delete();
                Files.copy(javaFile.toPath(), javaBackup.toPath());
                Files.copy(targetClassFile.toPath(), classBackup.toPath());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            log.debug("selected file = " + javaFilePath);

            ITree suspCodeAstNode = suspCodePair.getFirst(); //scp.getSuspiciousCodeAstNode();
            AstParser.AstNode node = suspCodePair.getSecond();
            String suspCodeStr = node.nodeStr; //scp.getSuspiciousCodeStr();
            int[] buggyLines = {node.startLine, node.endLine};
            log.debug("selected file Suspicious Code: \n" + suspCodeStr);

            suspCodeNode = new SuspCodeNode(javaBackup, classBackup, javaFile, targetClassFile,
                    node.startPos, node.endPos, suspCodeAstNode, suspCodeStr, javaFilePath, buggyLines);

        }
        return suspCodeNode;
    }

    public SuspCodeNode getSuspCodeNode(ErrorCallback errorCallback, ExcludeJavaFilter excludeScnFilter) {
        assert allJavaFiles != null && !allJavaFiles.isEmpty();

        if (LOADING_MODE.equals(LoadingMode.SHUFFLE_ALL)) return getNextSuspCodeNode(errorCallback, excludeScnFilter);

        SuspCodeNode suspCodeNode = null;
        // select a file
        File javaFile;
        List<Pair<ITree, AstParser.AstNode>> fileAst = null;
        FileLoadingException e = null;

        while (fileAst == null || fileAst.isEmpty() || e != null || suspCodeNode == null) {
            e = null;
            List<File> allJavaFilesList = new ArrayList<>(allJavaFiles);
            javaFile = randomItem(getRandom(), allJavaFilesList);
            if (!javaFile.exists()) {
                e = new FileLoadingException(javaFile);
                if (errorCallback != null) errorCallback.onError(e, javaFile, allJavaFiles);
                continue;
            }
            String javaFilePath = getSrcJavaFilePath(javaFile);
            if (excludeScnFilter != null && excludeScnFilter.shouldSkip(javaFilePath.substring(1))) {
                assert excludeScnFilter.includeOnly() != null && !excludeScnFilter.includeOnly().isEmpty();
                allJavaFiles = new HashSet<>();
                for (String s : excludeScnFilter.includeOnly()) {
                    File file = getFileFromSrcJavaPath(s);
                    assert file.exists();
                    allJavaFiles.add(file);
                }
                continue;
            }
            fileAst = getFileInterestingAstNodes(javaFile);

            if (fileAst == null || fileAst.isEmpty()) {
                e = new FileLoadingException(javaFile);
                if (errorCallback != null) errorCallback.onError(e, javaFile, allJavaFiles);
            } else {
                // todo test these methods
                String targetClassFilePath = projectClassPath + javaFilePath.substring(PathUtils.getSrcPath(this.buggyProject, defects4jPath, projectPath).get(2).length()).trim().replace(".java", ".class");
                File targetClassFile = new File(targetClassFilePath);
                File javaBackup = new File(FileUtils.tempJavaPath(javaFilePath, DATA_TYPE + "/" + this.buggyProject));
                File classBackup = new File(FileUtils.tempClassPath(javaFilePath, DATA_TYPE + "/" + buggyProject));
                try {
                    if (!targetClassFile.exists()) {
                        log.error("class not found = " + targetClassFile);
                        e = new MissingBinary(targetClassFile);
                        if (errorCallback == null || errorCallback.onError(e, javaFile, allJavaFiles))
                            continue;
                    }
                    assert targetClassFile.exists();
                    if (javaBackup.exists()) javaBackup.delete();
                    if (classBackup.exists()) classBackup.delete();
                    Files.copy(javaFile.toPath(), javaBackup.toPath());
                    Files.copy(targetClassFile.toPath(), classBackup.toPath());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                log.debug("selected file = " + javaFilePath);

                Pair<ITree, AstParser.AstNode> suspCodePair = randomItem(getRandom(), fileAst);
                ITree suspCodeAstNode = suspCodePair.getFirst(); //scp.getSuspiciousCodeAstNode();
                AstParser.AstNode node = suspCodePair.getSecond();
                int[] buggyLines = {node.startLine, node.endLine};
                log.debug("selected file Suspicious Code: \n" + node.nodeStr);
                suspCodeNode = new SuspCodeNode(javaBackup, classBackup, javaFile, targetClassFile,
                        node.startPos, node.endPos, suspCodeAstNode, node.nodeStr, javaFilePath, buggyLines);
            }
        }

        return suspCodeNode;
    }

    public boolean hasNodes() {
        return nodeFilePair == null || (LOADING_MODE.equals(LoadingMode.SHUFFLE_ALL) && !nodeFilePair.isEmpty());
    }

    public interface ExcludeJavaFilter {
        Set<String> includeOnly();

        //it has to be like src/main/java/org/apache/commons/lang3/math/NumberUtils.java
        boolean shouldSkip(String javaFile);
    }

    public interface ErrorCallback {

        boolean onError(RandomLocalisationProvider.FileLoadingException throwable, File file, Set<File> allJavaFiles);
    }

    public static class FileLoadingException extends Exception {

        protected String description = "Couldn't load the requested file.";
        public final File file;

        public FileLoadingException(File file) {
            this.file = file;
        }

        public FileLoadingException(String message, File file) {
            super(message);
            this.file = file;
        }

        public String print() {
            return description + " | " + file.getPath();
        }
    }

    public class MissingBinary extends FileLoadingException {

        private void initDescription() {
            description += " The .class file in missing in the target folder.";
        }

        public MissingBinary(File file) {
            super(file);
            initDescription();
        }

    }
}
