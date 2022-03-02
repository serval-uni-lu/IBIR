package edu.lu.uni.serval.ibir.config;

public class Configuration {

    public static final String DATA_TYPE = "MIMIC";
    public static String bugInjectionTargetFilePath;
	public static String outputPath = "OUTPUT/";
	public static int mutantsNumber = 5;
	public static final long SHELL_RUN_TIMEOUT_SEC = 300L;
	public static final String TEMP_FILES_PATH = System.getProperty("TEMP_FILES_PATH",".temp/");
}
