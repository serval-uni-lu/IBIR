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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.lu.uni.serval.ibir.output.CsvConsts.*;

/**
 * for a given d4jv2 project and id
 * load the csv of discarded patches
 * load the patches base64 one by one
 * apply the patch
 * compile it
 * keep a list of non compiling patches indexes output the indexes into a file
 * Devide the number of non compilable mutants by all mutants (valid + discarded) and print it to that csv file.
 */
public class NonCompilePatches {

    public static final boolean COMPILATION_CHECK = Boolean.getBoolean("COMPILATION_CHECK");
    private static final String CSV_HEADERS = "prjName,MAT_LEN,DISCARD_LEN,nonCompiling,nonCompilingRatio,remaining";
    private static final String CSV_SUFFIX = "discard.csv";
    private static Logger log = LoggerFactory.getLogger(NonCompilePatches.class);

    private final List<PatchEntry> nonCompilablePatches;
    private final Map<String, String> nonCompilableMutantsIdPattern;
    private final int validLength, discardedLength;
    private final Path discardFile;
    private final String pid_bid;
    private List<PatchEntry> allDiscardedPatches;


    public Path getDiscardedFilePath() {
        return discardFile;
    }

    public NonCompilePatches(String pid_bid) throws CsvValidationException, IOException {
        this.pid_bid = pid_bid;
        this.nonCompilablePatches = new ArrayList<>();
        this.nonCompilableMutantsIdPattern = new HashMap<>();
        this.discardFile = Paths.get(Configuration.outputPath, "discarded", "IBIR", pid_bid + CSV_SUFFIX);

        CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(discardFile.toFile()));
        Map<String, String> line = reader.readMap();
        if (line != null) {
            this.validLength = Integer.parseInt(line.get("MAT_LEN"));
            this.discardedLength = Integer.parseInt(line.get("DISCARD_LEN"));
        } else {
            throw new CsvValidationException("could not load stats");
        }
    }

    public void loadAllDiscardedPatches(ProgressPrinter progressPrinter) {
        this.allDiscardedPatches = new ArrayList<>(progressPrinter.getTriedPatchCandidates());
    }


    public PatchEntry addPatch(String fl_loc, String id, Patch patch, String patternName, String lineStart, String lineEnd) {
        PatchEntry dupl = getDuplicate(patch);
        boolean isDupl = dupl != null;
        if (!isDupl) {
            PatchEntry patchEntry = new PatchEntry(fl_loc, id, patch, lineStart, lineEnd);
            nonCompilableMutantsIdPattern.put(id, patternName);
            nonCompilablePatches.add(patchEntry);
            return null;
        } else {
            return dupl;
        }
    }

    private PatchEntry getDuplicate(Patch patch) {
        for (PatchEntry triedPatchCandidate : nonCompilablePatches) {
            // oh yes that's against java best-practices! but don't change it !!!
            // To know more why, check the implementation of equals... check Tbar...
            if (triedPatchCandidate.equals(patch)) return triedPatchCandidate;
        }
        return null;
    }


    public void printCsvs() throws IOException {
        int nonCompilingLength = nonCompilableMutantsIdPattern.size();
        Path outputPath = Paths.get(Configuration.outputPath, "discarded", "IBIR_compil").toAbsolutePath();
        File statsFile = outputPath.resolve(pid_bid + "stats.csv").toFile();
        File idPatternsFile = outputPath.resolve(pid_bid + "patches.csv").toFile();
        String stats = CSV_HEADERS + "\n"
                + pid_bid + "," +
                +validLength + "," +
                +discardedLength + "," +
                +nonCompilingLength + "," +
                (((float) nonCompilingLength) / ((float) validLength + discardedLength )) +","+ allDiscardedPatches.size();
        FileUtils.createFile(statsFile, stats);
        log.info("printed results to file :" + statsFile.getPath().toString());
        log.info("stats :");
        log.info(stats);
        StringBuilder str = new StringBuilder(PATCH_ID + "," + PATTERN + "\n");
        for (String id : nonCompilableMutantsIdPattern.keySet()) {
            str.append(id).append(",").append(nonCompilableMutantsIdPattern.get(id)).append("\n");
        }
        FileUtils.createFile(idPatternsFile, str.toString());
        log.info("printed results to file :" + idPatternsFile.getPath().toString());


    }

    public boolean isDone() {
        return allDiscardedPatches.isEmpty();
    }

    public PatchEntry findPatchEntry(Patch patch) {
        for (PatchEntry triedPatchCandidate : allDiscardedPatches) {
            // oh yes that's against java best-practices! but don't change it !!!
            // To know more why, check the implementation of equals... check Tbar...
            if (triedPatchCandidate.equals(patch)) return triedPatchCandidate;
        }
        return null;
    }

    public void removeFromInput(PatchEntry pe) {
        allDiscardedPatches.remove(pe);
    }
}
