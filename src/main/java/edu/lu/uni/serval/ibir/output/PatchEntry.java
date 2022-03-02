package edu.lu.uni.serval.ibir.output;

import edu.lu.uni.serval.tbar.info.Patch;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

import static edu.lu.uni.serval.ibir.output.CsvConsts.*;

public class PatchEntry {
    final String fl_loc;
    final String id;
    final Patch patch;
    final String duplicateId;
    private String csv;
    private String patchStr;

    PatchEntry(Map<String, String> csvLine) {
        this(csvLine.get(LOCALISATION_LINE), csvLine.get(PATCH_ID), csvLine.get(PATCH_OBJ), csvLine.get(DUPLICATED_PATCH_ID));
    }

    private PatchEntry(String fl_loc, String id, String patch, String duplicateId) {
        this.fl_loc = fl_loc;
        this.id = id;
        this.patch = patchFromStr(patch);
        this.duplicateId = duplicateId;
    }

    public PatchEntry(int fl_loc, int id, Patch patch){
        this(String.valueOf(fl_loc), String.valueOf(id), patch);
    }


    PatchEntry(String fl_loc, String id, Patch patch) {
        this.fl_loc = fl_loc;
        this.id = id;
        this.patch = patch;
        this.duplicateId = null;
        this.csv = null;
        this.patchStr = null;
    }

    public PatchEntry(String fl_loc, String id, Patch patch, PatchEntry dupl) {
        this.fl_loc = fl_loc;
        this.id = id;
        this.patch = patch;
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
