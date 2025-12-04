

/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.76.0

import io.javelit.core.Jt;
import model.Car;
import model.Owner;

public class Test {

  public static void main(String[] args) throws InterruptedException {
    Jt.title(String.valueOf(Car.BLUE)).use();
    Jt.title(String.valueOf(Owner.ME)).use();
    Jt.error("some error").use();
  }
}
