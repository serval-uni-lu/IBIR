package edu.lu.uni.serval.ibir.utils;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public final class PerfectClassLocalisationUtils {

    private static Logger log = LoggerFactory.getLogger(PerfectClassLocalisationUtils.class);

    private PerfectClassLocalisationUtils() {
        throw new IllegalAccessError("Utility class: No instance allowed, static access only.");
    }

    public static Set<String> loadPerfectLocalisationFiles(String csvDirPath, String projectName, String bugId, String srcPath) throws IOException, CsvValidationException {
        final String BUG_ID_KEY = "bug.id";
        final String MODIFIED_FILES_KEY = "classes.modified";

        CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(Paths.get(csvDirPath, String.format("%s_bugs.csv", projectName)).toAbsolutePath().toString()));
        Map<String, String> bugInfos;
        do {
            bugInfos = reader.readMap();
        } while (!bugId.equals(bugInfos.get(BUG_ID_KEY)));

        String res = bugInfos.get(MODIFIED_FILES_KEY);
        List<String> listRes = Arrays.asList(res.split(";"));
        return new HashSet<String>() {{
            for (String file : listRes) {
                if (!file.startsWith(srcPath)) {
                    file = srcPath + file;
                }
                if (file.startsWith("/"))
                {
                   file = file.substring(1);
                }
                if (!file.endsWith(".txt")) {
                    add(file.replace(".", "/") + ".java");
                }
            }
        }};
    }

}
