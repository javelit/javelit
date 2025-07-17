package tech.catheu.jeamlit.core;

public interface JtComponentBuilder<T extends JtComponent> {
    T build();
}