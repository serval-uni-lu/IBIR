package edu.lu.uni.serval.ibir.shell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


public final class EnvironmentPath {

    private static Logger log = LoggerFactory.getLogger(EnvironmentPath.class);

    private static String[] defaultEnvArray;

    public static String JAVA_HOME;

    private EnvironmentPath() {
        throw new IllegalAccessError("Utility class: No instance allowed, static access only.");
    }

    private static String[] getDefaultEnvArray(String javaHomePath) {
        if (defaultEnvArray == null) {
            javaHomePath =  checkPassedJavaHomePath(javaHomePath);
            Map<String, String> envMap = cloneEnv();
            String envPath = envMap.get("PATH");
            log.debug("env path = " + envPath);
            log.debug("env javaHomePath = " + javaHomePath);
            envMap.put("JAVA_HOME", javaHomePath);
            envMap.put("PATH", javaHomePath + System.getProperty("path.separator") + envPath);
            defaultEnvArray = new String[envMap.entrySet().size()];
            int i = 0;
            for (Map.Entry<String, String> stringStringEntry : envMap.entrySet()) {
                defaultEnvArray[i] = stringStringEntry.getKey() + "=" + stringStringEntry.getValue();
                i++;
            }
            log.debug("---> loaded envMap = " + envMap);
        }
        return defaultEnvArray;
    }

    /**
     *
     * Quick and dirty improvement of user experience with most of java home issues.
     *
     * @param javaHomePath the inputed java home path.
     * @return same or fixed path if it was not pointing to Home.
     */
    private static String checkPassedJavaHomePath(String javaHomePath) {
        if (javaHomePath == null || javaHomePath.isEmpty()) {
            throw new IllegalArgumentException("no java home passed");
        } else if (!javaHomePath.contains("Home")) {
            log.warn("I think you should check your java home path because it doesn't have any Home dir : " + javaHomePath);
        } else if (!(javaHomePath.contains("8"))) {
            log.warn("I think you should check your system java home path because it doesn't seem to be of version 7 or 8 : " + javaHomePath);
        } else if (!javaHomePath.endsWith("Home")) {
            log.warn("Your java home path doesn't point to the directory home : " + javaHomePath);
            javaHomePath = javaHomePath.substring(0, javaHomePath.indexOf("Home") + "Home".length());
        }
        return javaHomePath;
    }

    public static String[] getDefaultEnvArray() {
        // System.out.println("passed java home = " + JAVA_HOME);
        String javaHome = JAVA_HOME == null || JAVA_HOME.isEmpty()? System.getProperty("java.home") : JAVA_HOME;
        return getDefaultEnvArray(javaHome);
    }

    public static Map<String, String> cloneEnv() {
        return cloneEnv(new ProcessBuilder());
    }

    public static Map<String, String> cloneEnv(ProcessBuilder processBuilder) {
        return new HashMap<>(processBuilder.environment());
    }
}
