/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.76.0


import io.javelit.core.Jt;

public class ExpanderExample {

  public static void main(String[] args) throws InterruptedException {
    final var expanderC = Jt.expander("See explanation").use();
    Jt.title("THE EXPLANATION TITLE").use(expanderC);
    Jt.text("Ita fac, no way: Vindica te tibi, et tempus. Lorem ipsum.").use(expanderC);
  }
}
