package edu.lu.uni.serval.ibir.utils;

import com.opencsv.exceptions.CsvValidationException;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static edu.lu.uni.serval.ibir.utils.PerfectClassLocalisationUtils.loadPerfectLocalisationFiles;
import static org.junit.Assert.*;

public class PerfectClassLocalisationUtilsTest {

    @Test
    public void loadPerfectLocalisationFilesTest_Closue() throws CsvValidationException, IOException {
        List<String> expected = Arrays.asList("src/org/apache/commons/math3/fraction/BigFraction.java","src/org/apache/commons/math3/fraction/Fraction.java");
        String csvDirPath = "input/d4j_v2/evaluation/bugs", projectName = "Math", bugId = "1", srcPath = "/src/";
        Set<String> pclfiles = loadPerfectLocalisationFiles(csvDirPath, projectName, bugId, srcPath);
        assertNotNull(pclfiles);
        assertFalse(pclfiles.isEmpty());
        assertArrayEquals(expected.toArray(), pclfiles.toArray());
    }

    @Test
    public void loadPerfectLocalisationFilesTest_Cli() throws CsvValidationException, IOException {
        List<String> expected = Arrays.asList("src/java/org/apache/commons/cli/CommandLine.java");
        String csvDirPath = "input/d4j_v2/evaluation/bugs", projectName = "Cli", bugId = "1", srcPath = "/src/java/";
        Set<String> pclfiles = loadPerfectLocalisationFiles(csvDirPath, projectName, bugId, srcPath);
        assertNotNull(pclfiles);
        assertFalse(pclfiles.isEmpty());
        assertArrayEquals(expected.toArray(), pclfiles.toArray());
    }
}