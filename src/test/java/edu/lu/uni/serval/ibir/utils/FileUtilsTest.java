package edu.lu.uni.serval.ibir.utils;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class FileUtilsTest {

    @Test
    public void fileLines() throws IOException {
      Assert.assertEquals(1019,FileUtils.fileLines("src/test/resources/INPUT/FileUtilsTest/Math_98/all_tests"));
    }
}