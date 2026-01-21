/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.78.0


import java.util.List;

import io.javelit.components.layout.ColumnsComponent;
import io.javelit.core.Jt;

public class TitleExample {

  public static void main(String[] args) throws InterruptedException {
    Jt.title("This is a title").use();
    Jt.title("_Javelit_ is cool :sunglasses:").use();
    Jt.header("This is a header").use();
    Jt.header("_Javelit_ is cool :sunglasses:").use();
    Jt.subheader("This is a subheader").use();
    Jt.subheader("_Javelit_ is cool :sunglasses:").use();
  }
}
