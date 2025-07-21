package tech.catheu.jeamlit.exception;

import tech.catheu.jeamlit.spi.JtComponent;

/**
 * Exception thrown when two widgets have the same automatically generated or explicit key.
 * This mimics Streamlit's DuplicateWidgetID error behavior.
 */
public class DuplicateWidgetIDException extends RuntimeException {

    public DuplicateWidgetIDException(String message) {
        super(message);
    }

    public static DuplicateWidgetIDException of(final JtComponent<?> component) {
        return new DuplicateWidgetIDException(String.format(
                "There are multiple identical %s widgets with the same key='%s'. " + "To fix this, please pass a unique key argument to each widget.",
                component.getClass().getName(),
                component.getKey()));
    }
}