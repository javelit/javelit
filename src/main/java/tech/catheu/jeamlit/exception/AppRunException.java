package tech.catheu.jeamlit.exception;

public class AppRunException extends RuntimeException {

    public AppRunException(Exception e) {
        super(e);
    }
}
