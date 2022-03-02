package edu.lu.uni.serval.ibir;

public class InjectionAbortingException extends Exception {

    public InjectionAbortingException(String message) {
        super(message);
    }

    public InjectionAbortingException(Throwable cause) {
        super(cause);
    }
}
