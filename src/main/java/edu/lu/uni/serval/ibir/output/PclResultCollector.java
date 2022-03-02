package edu.lu.uni.serval.ibir.output;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;


/**
 * <p>
 */
public class PclResultCollector extends ResultCollector {

    private static Logger log = LoggerFactory.getLogger(PclResultCollector.class);
    private static final String DEFAULT_IBIR_PCL_MATRICES_SUBDIRECTORY = "ibirPcl_mutation_mat";

    private final Set<String> pclFiles;

    public PclResultCollector(int numberOfMutants, Set<String> pclFiles, Set<String> testsBrokenByOriginalBug, String projectName_bugId) {
        super(numberOfMutants, testsBrokenByOriginalBug, projectName_bugId);
        this.pclFiles = pclFiles;
        this.subdirectory =  matricesSubDirectoryPrefix + DEFAULT_IBIR_PCL_MATRICES_SUBDIRECTORY;
        assert pclFiles != null && !pclFiles.isEmpty();
    }

    @Override
    public void addPatch(MutantPatch mutantPatch, long elapsedTime) throws IOException {
        if (isPcl(mutantPatch)) {
            super.addPatch(mutantPatch, elapsedTime);
        }
    }

    public boolean shouldSkip(String classPath) {
        return isBudgetConsumed() || !pclFiles.contains(classPath);
    }

    public boolean isPcl(String classPath) {
        assert pclFiles != null && !pclFiles.isEmpty();
        return pclFiles.contains(classPath);
    }

    private boolean isPcl(MutantPatch mutantPatch) {
        assert pclFiles != null && !pclFiles.isEmpty();
        return pclFiles.contains(mutantPatch.getPrioSuspeciousPosition().classPath);
    }

    @Override
    public Set<String> getPclFiles() {
        return pclFiles;
    }

}
