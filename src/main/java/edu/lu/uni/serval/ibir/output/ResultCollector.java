package edu.lu.uni.serval.ibir.output;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;
import edu.lu.uni.serval.ibir.config.Configuration;
import edu.lu.uni.serval.ibir.utils.FileUtils;
import edu.lu.uni.serval.tbar.info.Patch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static edu.lu.uni.serval.ibir.output.CsvConsts.*;
import static edu.lu.uni.serval.ibir.utils.FileUtils.appendLineToFile;

/**
 * list of compilable patches.
 */
public class ResultCollector {

    private static final Logger log = LoggerFactory.getLogger(ResultCollector.class);
    public static final String DEFAULT_IBIR_MATRICES_SUBDIRECTORY = "ibir_mutation_mat";

    protected final int numberOfMutants;

    protected final List<PatchEntry> compilablePatchEntries;
    private final String projectName_bugId;
    protected File patchesByLocationFile, mutationMatFile;
    protected String matricesSubDirectoryPrefix = Boolean.getBoolean("extraRemovingPatternsEnabled") ? "ExRP_" : "";
    protected Set<String> testsBrokenByOriginalBug;
    protected String subdirectory;

    public ResultCollector(int numberOfMutants, Set<String> testsBrokenByOriginalBug, String projectName_bugId) {
        this.numberOfMutants = numberOfMutants;
        this.testsBrokenByOriginalBug = testsBrokenByOriginalBug;
        this.compilablePatchEntries = new ArrayList<>();
        this.subdirectory = matricesSubDirectoryPrefix + DEFAULT_IBIR_MATRICES_SUBDIRECTORY;
        this.projectName_bugId = projectName_bugId;
        log.info("============ budget set : number of mutants ============  " + numberOfMutants + "  ============");
    }

    public void init() throws IOException, CsvValidationException {
        this.mutationMatFile = Paths.get(Configuration.outputPath).resolve(subdirectory).resolve(projectName_bugId + DEFAULT_MUTATION_MATRIX_FILE_SUFFIX).toFile();
        this.patchesByLocationFile = Paths.get(mutationMatFile.getPath()).getParent().resolve("patchesByLocation").resolve(projectName_bugId + DEFAULT_PATCHES_BY_LOCATION_FILE_SUFFIX).toFile();

        // prepare results dir.
        if (!mutationMatFile.exists() || mutationMatFile.length() == 0) {
            FileUtils.createFile(mutationMatFile, DEFAULT_MUTATION_MATRIX_CSV_HEADERS + "\n");
        } else {
            loadPreviouslyGeneratedCompilablePatchesIds();
        }
        if (!patchesByLocationFile.exists() || patchesByLocationFile.length() == 0) {
            FileUtils.createFile(patchesByLocationFile, DEFAULT_PATCHES_BY_LOCATION_FILE_HEADERS + "\n");
        }
    }

    private void loadPreviouslyGeneratedCompilablePatchesIds() throws IOException, CsvValidationException {
        CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(mutationMatFile));
        Map<String, String> line = reader.readMap();
        while (line != null) {
            compilablePatchEntries.add(new PatchEntry(line));
            line = reader.readMap();
        }
    }

    public boolean shouldSkip(String classPath) {
        return isBudgetConsumed();
    }

    public void addPatch(MutantPatch mutantPatch, long elapsedTime) throws IOException {
        if (isBudgetConsumed()) return;
        PatchEntry ancestorPatchEntry = null;
        for (PatchEntry compilablePatchId : compilablePatchEntries) {
            if (compilablePatchId.equals(mutantPatch.getPatch())) {
                ancestorPatchEntry = compilablePatchId;
                break;
            }
        }

        if (ancestorPatchEntry == null) {
            PatchEntry mutantPatchEntry = new PatchEntry(mutantPatch.getPrioSuspeciousPosition().localisationResultLine, mutantPatch.getPatchId(), mutantPatch.getPatch());
            compilablePatchEntries.add(mutantPatchEntry);
            printToMutationMat(mutantPatch, elapsedTime);
            printToLocations(mutantPatchEntry.fl_loc, mutantPatchEntry.id);
        } else {
            String mutantLoc = String.valueOf(mutantPatch.getPrioSuspeciousPosition().localisationResultLine);
            printToLocations(mutantLoc, ancestorPatchEntry.id);
        }
        log.info("============ generated " + compilablePatchEntries.size() + " patches / " + numberOfMutants);
    }

    public PatchEntry exists(PatchEntry entry) throws IOException {
        for (PatchEntry compilablePatchEntry : compilablePatchEntries) {
            if (compilablePatchEntry.equals(entry.patch)) {
                return compilablePatchEntry;
            }
        }
        return null;
    }

    private void printToMutationMat(MutantPatch patch, long elapsedTime) throws IOException {
        appendLineToFile(mutationMatFile, patch.toCsv(testsBrokenByOriginalBug, elapsedTime));
    }

    public void printToLocations(int loc, PatchEntry patchEntry) throws IOException {
        printToLocations(loc, patchEntry.id);
    }

    public void printToLocations(int loc, String patchId) throws IOException {
        appendLineToFile(patchesByLocationFile, loc + "," + patchId);
    }


    private void printToLocations(int loc, int patchId) throws IOException {
        appendLineToFile(patchesByLocationFile, loc + "," + patchId);
    }

    public void printToLocations(String loc, String patchId) throws IOException {
        appendLineToFile(patchesByLocationFile, loc + "," + patchId);
    }

    public boolean isBudgetConsumed() {
        return numberOfMutants <= compilablePatchEntries.size();
    }

    public Set<String> getPclFiles() {
        throw new IllegalStateException("default result collector doesn't need PCL. Implement this and don't call super().");
    }

    public Map<String, String> getCsvLine(PatchEntry patchEntry) throws IOException, CsvValidationException {
        CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(mutationMatFile));
        Map<String, String> line = reader.readMap();
        while (line != null && !patchEntry.id.equals(line.get(PATCH_ID))) {
            line = reader.readMap();
        }
        return line;
    }

    public void addPatch(int localisationResultLine, int patchId, Patch patch, Map<String, String> csvLine, String conf) throws IOException {
        if (isBudgetConsumed()) return;
        PatchEntry mutantPatchEntry = new PatchEntry(localisationResultLine, patchId, patch);
        compilablePatchEntries.add(mutantPatchEntry);
        String csvLineStr = csvLine.get(BUG_ID) + "," +
                localisationResultLine + "," +
                conf + "," +
                patchId + "," +
                csvLine.get(brokenTestsCount) + "," +
                csvLine.get(PATTERN) + "," +
                csvLine.get(OchiaiCoef) + "," +
                csvLine.get(sameBrokenTestsAsOriginalBug) + "," +
                csvLine.get(breaksOnlySubsetOfBrokenTestsByOriginalBug) + "," +
                csvLine.get(brokenTests) + "," +
                csvLine.get(PATCH_OBJ);
        appendLineToFile(mutationMatFile, csvLineStr);
        printToLocations(localisationResultLine, patchId);
    }

}
