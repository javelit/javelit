package tech.catheu.jeamlit.core;

// note: not a fan of these custom exception but I'll start with this for the moment
// previously been working go style with records results containing success bool and error message but it was not much better
///  Exception thrown when compilation of the app fails OR when launching the main method fails
class CompilationException extends RuntimeException {
    protected CompilationException(String message) {
        super(message);
    }
}
