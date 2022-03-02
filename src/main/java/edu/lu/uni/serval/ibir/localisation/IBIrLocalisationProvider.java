package edu.lu.uni.serval.ibir.localisation;

import edu.lu.uni.serval.tbar.utils.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 *
 //0,CLOSURE-253,src/com/google/javascript/jscomp/OptimizeReturns.java,0.23481676615063515,['44-44']
 //7,CLOSURE-253,src/com/google/javascript/jscomp/Compiler.java,0.15470502888342288,"['588-588', '623-623']"
 */
public class IBIrLocalisationProvider {

    private static Logger log = LoggerFactory.getLogger(IBIrLocalisationProvider.class);

    /**
     * 2 = 1 line for header + 1 Line for a localisation entry.
     */
    private static final int MINIMUM_LOCALISATION_FILE_LENGTH = 2;
    private final File localisationFile;

    public static IBIrLocalisationProvider newInstance(String filePath) throws LocalisationFailedException {
        log.info("loading localisation file = " + filePath);
        File localisationFile = getFile(filePath);
        return new IBIrLocalisationProvider(localisationFile);
    }

    private static File getFile(String localisationFilePath) throws LocalisationFailedException {
        assert localisationFilePath != null && !localisationFilePath.isEmpty();
        File localisationFile = new File(localisationFilePath);
        if (!localisationFile.exists() || !localisationFile.isFile()) {
            throw new LocalisationFailedException("Cannot find the bug positions file: " + localisationFile);
        }
        return localisationFile;
    }

    private IBIrLocalisationProvider(File localisationFile) {
        this.localisationFile = localisationFile;
    }


    public List<PrioSuspeciousPosition> getSuspeciousCodePositions() throws LocalisationFailedException {

        assert localisationFile != null && localisationFile.exists() && localisationFile.isFile();

        String[] ibirLocalisations = FileHelper.readFile(localisationFile).split("\n");

        if (ibirLocalisations == null || ibirLocalisations.length < MINIMUM_LOCALISATION_FILE_LENGTH ) {
            throw new LocalisationFailedException("Cannot read the bug positions file: " + localisationFile);
        }

        // remove the header line.
        ibirLocalisations = Arrays.copyOfRange(ibirLocalisations, 1, ibirLocalisations.length);

        List<PrioSuspeciousPosition> suspiciousCodeList = new ArrayList<>();
        for (String line : ibirLocalisations) {

            String[] elements = line.split(",");

            int priority = Integer.valueOf(elements[0]);
            String confidence = elements[3];

            for (int index = 4, length = elements.length; index < length; index++) {
                String[] pos = elements[index].split("-");
                int startPos = Integer.parseInt(pos[0].substring(pos[0].indexOf("'") + 1));
//            		int endPos = Integer.parseInt(pos[1].substring(0, pos[1].indexOf("'")));
//            		for (int lineNumber = startPos; lineNumber <= endPos; lineNumber ++) {
                PrioSuspeciousPosition sp = new PrioSuspeciousPosition(elements[2], startPos, confidence, priority);
                suspiciousCodeList.add(sp);
//            		}
            }
        }

        if (suspiciousCodeList.isEmpty()) {
            throw new LocalisationFailedException("the bug positions file is empty: " + localisationFile);
        }
        return suspiciousCodeList;
    }
}
