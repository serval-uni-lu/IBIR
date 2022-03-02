package edu.lu.uni.serval.ibir.info;

import edu.lu.uni.serval.tbar.info.Patch;

import java.util.Objects;

public class IBIrPatch extends Patch {

    private boolean needBugyCode = true; // this was added just to don't break the existing logic.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (getClass() != o.getClass()) {
            if (o instanceof Patch && needBugyCode && o.equals(this)) return true;
            return false;
        }
        if (!super.equals(o)) return false;
        IBIrPatch ibIrPatch = (IBIrPatch) o;
        return needBugyCode == ibIrPatch.needBugyCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBuggyCodeStr(), getFixedCodeStr1(), getFixedCodeStr2(), getBuggyCodeStartPos(), getBuggyCodeEndPos(), buggyFileName, needBugyCode);
    }

    public void setNeedBugyCode(boolean needBugyCode) {
        this.needBugyCode = needBugyCode;
    }

    public boolean getNeedBugyCode() {
        return needBugyCode;
    }
}
