package edu.lu.uni.serval.ibir.localisation;

public class LocalisationFailedException extends Exception {

    public LocalisationFailedException() {
    }

    public LocalisationFailedException(String message) {
        super(message);
    }

    public LocalisationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
