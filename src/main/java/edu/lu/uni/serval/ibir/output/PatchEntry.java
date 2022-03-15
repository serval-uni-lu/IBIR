package edu.lu.uni.serval.ibir.output;

import edu.lu.uni.serval.tbar.info.Patch;

import java.util.Map;
import java.util.Objects;

import static edu.lu.uni.serval.ibir.output.CsvConsts.*;

public class PatchEntry {
    final String fl_loc;
    final String id;
    final Patch patch;
    final String duplicateId;
    final String lineStart;
    final String lineEnd;
    private String csv;
    private String patchStr;

    PatchEntry(Map<String, String> csvLine) {
        this(csvLine.get(LOCALISATION_LINE), csvLine.get(PATCH_ID), csvLine.get(PATCH_OBJ), csvLine.get(DUPLICATED_PATCH_ID),
                csvLine.get(LINE_START), csvLine.get(LINE_END));
    }

    private PatchEntry(String fl_loc, String id, String patch, String duplicateId, String lineStart, String lineEnd) {
        this.fl_loc = fl_loc;
        this.id = id;
        this.patch = patchFromStr(patch);
        this.duplicateId = duplicateId;
        this.lineStart = lineStart;
        this.lineEnd = lineEnd;
    }

    public PatchEntry(int fl_loc, int id, Patch patch, int lineStart, int lineEnd){
        this(String.valueOf(fl_loc), String.valueOf(id), patch, String.valueOf(lineStart), String.valueOf(lineEnd));
    }


    PatchEntry(String fl_loc, String id, Patch patch, String lineStart, String lineEnd) {
        this.fl_loc = fl_loc;
        this.id = id;
        this.patch = patch;
        this.lineStart = lineStart;
        this.lineEnd = lineEnd;
        this.duplicateId = null;
        this.csv = null;
        this.patchStr = null;
    }

    public PatchEntry(String fl_loc, String id, Patch patch, PatchEntry dupl, String lineStart, String lineEnd) {
        this.fl_loc = fl_loc;
        this.id = id;
        this.patch = patch;
        this.lineStart = lineStart;
        this.lineEnd = lineEnd;
        this.csv = null;
        if (dupl != null) {
            this.duplicateId = dupl.id;
            this.patchStr = dupl.patchStr;
        } else {
            this.duplicateId = null;
            this.patchStr = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (o instanceof Patch) return comparePatches((Patch) o);
        if (getClass() != o.getClass()) return false;
        PatchEntry that = (PatchEntry) o;
        return Objects.equals(fl_loc, that.fl_loc) && Objects.equals(id, that.id) && Objects.equals(duplicateId, that.duplicateId) && Objects.equals(patch, that.patch);
    }

    private boolean comparePatches(Patch o) {
        return Objects.equals(patch, o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fl_loc, id, patch, duplicateId);
    }

    public String toCsv() {
        if (csv == null) {
            String patchStr = patchToStr(patch);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(fl_loc).append(CSV_SEPARATOR)
                    .append(id).append(CSV_SEPARATOR);
            if (duplicateId == null)
                stringBuilder.append("\"").append(patchStr).append("\"").append(CSV_SEPARATOR);
            else
                stringBuilder.append(CSV_SEPARATOR)
                        .append(duplicateId);
            stringBuilder.append(CSV_SEPARATOR).append(lineStart).append(CSV_SEPARATOR).append(lineEnd).append(CSV_SEPARATOR).append(patch.buggyFileName);
            csv = stringBuilder.toString();
        }
        return csv;
    }

    public String patchToStr(Patch patch) {
        if (patchStr == null) {
            patchStr = Serializer.to(patch);
        }
        return patchStr;
    }

    public Patch patchFromStr(String str) {
        return Serializer.from(str, Patch.class);
    }
}
