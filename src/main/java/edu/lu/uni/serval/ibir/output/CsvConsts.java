package edu.lu.uni.serval.ibir.output;

public class CsvConsts {


    public static final String PATCH_ID = "patchId";
    public static final String LOCALISATION_LINE = "localisationLine";
    public static final String FILE = "file";
    public static final String LINE_START = "lineStart";
    public static final String LINE_END = "lineEnd";
    public static final String CONFIDENCE = "confidence";
    public static final String BUG_ID = "prjName";
    public static final String DUPLICATED_PATCH_ID = "duplPatchID";
    public static final String PATCH_OBJ = "patchObj";
    public static final String brokenTestsCount = "brokenTestsCount";
    public static final String PATTERN = "pattern";
    public static final String OchiaiCoef = "OchiaiCoef";
    public static final String sameBrokenTestsAsOriginalBug = "sameBrokenTestsAsOriginalBug";
    public static final String breaksOnlySubsetOfBrokenTestsByOriginalBug = "breaksOnlySubsetOfBrokenTestsByOriginalBug";
    public static final String brokenTests = "brokenTests";
    public static final String DURATION_COLUMN = "duration";

    public static final String CSV_SEPARATOR = ",";
    public static final String PATCH_ENTRY_CSV_HEADER = LOCALISATION_LINE + CSV_SEPARATOR + PATCH_ID + CSV_SEPARATOR + PATCH_OBJ + CSV_SEPARATOR + DUPLICATED_PATCH_ID
            + CSV_SEPARATOR + LINE_START
            + CSV_SEPARATOR + LINE_END
            + CSV_SEPARATOR + FILE
            + CSV_SEPARATOR + DURATION_COLUMN;

    public static final String DEFAULT_MUTATION_MATRIX_CSV_HEADERS = BUG_ID + "," + LOCALISATION_LINE + "," + CONFIDENCE + "," + PATCH_ID + "," + brokenTestsCount + "," + PATTERN + "," + OchiaiCoef + "," + sameBrokenTestsAsOriginalBug + "," + breaksOnlySubsetOfBrokenTestsByOriginalBug + "," + brokenTests + "," + PATCH_OBJ
            + CSV_SEPARATOR + DURATION_COLUMN
            + CSV_SEPARATOR + LINE_START
            + CSV_SEPARATOR + LINE_END
            + CSV_SEPARATOR + FILE;
    public static final String DEFAULT_MUTATION_MATRIX_FILE_SUFFIX = "_mat.csv";
    public static final String DEFAULT_PATCHES_BY_LOCATION_FILE_SUFFIX = "_PbL.csv";
    public static final String DEFAULT_PATCHES_BY_LOCATION_FILE_HEADERS = "localisationLine,patchId" + "," + LINE_START + "," + LINE_END+","+FILE;
}
