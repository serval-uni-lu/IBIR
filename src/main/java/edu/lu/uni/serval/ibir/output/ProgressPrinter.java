package edu.lu.uni.serval.ibir.output;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;
import edu.lu.uni.serval.ibir.utils.FileUtils;
import edu.lu.uni.serval.tbar.info.Patch;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static edu.lu.uni.serval.ibir.output.CsvConsts.*;
import static edu.lu.uni.serval.ibir.utils.FileUtils.appendLineToFile;

public class ProgressPrinter {

    private final List<PatchEntry> triedPatchCandidates;
    private final File file;

    public ProgressPrinter(String progressFilePath) {
        this.file = new File(progressFilePath);
        triedPatchCandidates = new ArrayList<>();
    }

    public void init() throws IOException, CsvValidationException {
        // prepare results dir.
        if (!file.exists() || file.length() == 0) {
            FileUtils.createFile(file, PATCH_ENTRY_CSV_HEADER + "\n");
        } else {
            loadPreviouslyGeneratedPatches();
        }
    }

    List<PatchEntry> getTriedPatchCandidates(){
        return triedPatchCandidates;
    }

    private void loadPreviouslyGeneratedPatches() throws IOException, CsvValidationException {
        CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(file));
        Map<String, String> line = reader.readMap();
        while (line != null) {
            PatchEntry entry = new PatchEntry(line);
            if (entry.duplicateId == null || entry.duplicateId.length() == 0 || "null".equals(entry.duplicateId))
                triedPatchCandidates.add(entry);
            line = reader.readMap();
        }
    }


    public PatchEntry addPatch(int[] lineNumbers, int fl_loc, int id, Patch patch, long elapsedTime) throws IOException {
        return addPatch(lineNumbers, String.valueOf(fl_loc), String.valueOf(id), patch, elapsedTime);
    }

    public PatchEntry addPatch(int[] lineNumbers, String fl_loc, String id, Patch patch, long elapsedTime) throws IOException {
        PatchEntry dupl = getDuplicate(patch);
        boolean isDupl = dupl != null;
        String lineStart = String.valueOf(lineNumbers[0]);
        String lineEnd = String.valueOf(lineNumbers[1]);
        PatchEntry patchEntry = isDupl ? new PatchEntry(fl_loc, id, patch, dupl, lineStart,lineEnd) : new PatchEntry(fl_loc, id, patch,  lineStart,lineEnd);
        printPatch(patchEntry, elapsedTime);
        if (!isDupl) {
            triedPatchCandidates.add(patchEntry);
            return null;
        } else {
            return dupl;
        }
    }

    private void printPatch(PatchEntry patchEntry, long elapsedTime) throws IOException {
        appendLineToFile(file, patchEntry.toCsv() + "," + elapsedTime);
    }

    private PatchEntry getDuplicate(Patch patch) {
        for (PatchEntry triedPatchCandidate : triedPatchCandidates) {
            // oh yes that's against java best-practices! but don't change it !!!
            // To know more why, check the implementation of equals... check Tbar...
            if (triedPatchCandidate.equals(patch)) return triedPatchCandidate;
        }
        return null;
    }

    public List<Patch> removeTriedOnes(List<Patch> patchCandidates) {
        for (PatchEntry triedPatchCandidate : triedPatchCandidates) {
            patchCandidates.remove(triedPatchCandidate.patch);
        }
        return patchCandidates;
    }

    public int triedPatchesCount() {
        return triedPatchCandidates.size();
    }
}
