package edu.lu.uni.serval.ibir.output;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;
import edu.lu.uni.serval.ibir.config.Configuration;
import edu.lu.uni.serval.ibir.localisation.IbirSuspiciousPosition;
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
    protected final boolean exhaustiveInjection;

    public ResultCollector(int numberOfMutants, Set<String> testsBrokenByOriginalBug, String projectName_bugId, boolean exhaustiveInjection) {
        this.numberOfMutants = numberOfMutants;
        this.testsBrokenByOriginalBug = testsBrokenByOriginalBug;
        this.exhaustiveInjection = exhaustiveInjection;
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

        IbirSuspiciousPosition mutantPosition = mutantPatch.getPrioSuspeciousPosition();


        if (ancestorPatchEntry == null) {
            PatchEntry mutantPatchEntry = new PatchEntry(mutantPosition.getLocalisationResultLine(), mutantPatch.getPatchId(), mutantPatch.getPatch(), mutantPosition.getLineStart(), mutantPosition.getLineEnd());
            compilablePatchEntries.add(mutantPatchEntry);
            printToMutationMat(mutantPatch, elapsedTime);
            printToLocations(mutantPatchEntry);
        } else {
            String mutantLoc = String.valueOf(mutantPosition.getLocalisationResultLine());
            printToLocations(mutantLoc, ancestorPatchEntry.id, String.valueOf(mutantPosition.getLineStart()), String.valueOf(mutantPosition.getLineEnd()), mutantPosition.classPath);
        }
        log.info("============ generated " + compilablePatchEntries.size() + " patches / " + (exhaustiveInjection ? "ALL" : "" + numberOfMutants));
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

    public void printDuplicateToLocations(int loc, PatchEntry patchEntry, int lineStart, int lineEnd, String fileName) throws IOException {
        appendLineToFile(patchesByLocationFile, loc + "," + patchEntry.id + "," + lineStart + "," + lineEnd+ "," + fileName);
    }

    public void printToLocations(PatchEntry patchEntry) throws IOException {
        printToLocations(patchEntry.fl_loc, patchEntry.id, patchEntry.lineStart, patchEntry.lineEnd, patchEntry.patch.buggyFileName);
    }


    private void printToLocations(int loc, int patchId, int lineStart, int lineEnd, String fileName) throws IOException {
        appendLineToFile(patchesByLocationFile, loc + "," + patchId + "," + lineStart + "," + lineEnd+ "," + fileName);
    }

    public void printToLocations(String fl_loc, String patchId, String lineStart, String lineEnd, String fileName) throws IOException {
        appendLineToFile(patchesByLocationFile, fl_loc + "," + patchId + "," + lineStart + "," + lineEnd + "," + fileName);
    }

    public boolean isBudgetConsumed() {
        return !exhaustiveInjection && numberOfMutants <= compilablePatchEntries.size();
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

    public void addPatch(int localisationResultLine, int patchId, Patch patch, Map<String, String> csvLine, String conf, int lineStart, int lineEnd) throws IOException {
        if (isBudgetConsumed()) return;
        PatchEntry mutantPatchEntry = new PatchEntry(localisationResultLine, patchId, patch, lineStart, lineEnd);
        compilablePatchEntries.add(mutantPatchEntry);
        String file = csvLine.get(FILE);
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
                csvLine.get(PATCH_OBJ) + "," +
                lineStart + "," +
                lineEnd + "," +
                file;
        appendLineToFile(mutationMatFile, csvLineStr);
        printToLocations(localisationResultLine, patchId, lineStart, lineEnd, file);
    }

}
