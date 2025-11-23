/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.74.0

import io.javelit.core.Jt;

public class TextExample {

  public static void main(String[] args) {
    Jt.text("Hello, World!").use();
    Jt.text("This is a simple text example.").use();
  }
}
