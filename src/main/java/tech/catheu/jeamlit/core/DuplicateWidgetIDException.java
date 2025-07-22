package tech.catheu.jeamlit.core;

/**
 * Exception thrown when two widgets have the same automatically generated or explicit key.
 * This mimics Streamlit's DuplicateWidgetID error behavior.
 */
class DuplicateWidgetIDException extends RuntimeException {

    private DuplicateWidgetIDException(String message) {
        super(message);
    }

    protected static DuplicateWidgetIDException of(final JtComponent<?> component) {
        return new DuplicateWidgetIDException(String.format(
                "There are multiple identical %s widgets with the same key='%s'. " + "To fix this, please pass a unique key argument to each widget.",
                component.getClass().getName(),
                component.getKey()));
    }
}