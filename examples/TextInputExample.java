/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.73.0


import java.util.List;

import io.javelit.components.layout.ColumnsComponent;
import io.javelit.core.Jt;
import io.javelit.core.JtComponent;

public class TextInputExample {

  public static void main(String[] args) throws InterruptedException {
    String text = Jt.textInput("How are you today ?").use();
    if (text != null && !text.isEmpty()) {
      Jt.text("Today, you are %s!".formatted(text)).use();
    }

    String text2 = Jt.textInput("How are you today ? In less than 10 characters").maxChars(10).use();
    if (text2 != null && !text2.isEmpty()) {
      Jt.text("Today, you are %s!".formatted(text2)).key("t2").use();
    }

    String text3 = Jt.textInput("How are you today ? But it's a password").type("password").use();
    if (text3 != null && !text3.isEmpty()) {
      Jt.text("Today, you are %s!".formatted(text3)).key("t3").use();
    }

    String text4 = Jt.textInput("How are you today ? With some inspiration").placeholder("I'm good!").use();
    if (text4 != null && !text4.isEmpty()) {
      Jt.text("Today, you are %s!".formatted(text4)).key("t4").use();
    }

    String text5 = Jt.textInput("How are you today ? See help").help("answer whether you are good or not").use();
    if (text5 != null && !text5.isEmpty()) {
      Jt.text("Today, you are %s!".formatted(text5)).key("t5").use();
    }

    String text7 = Jt
        .textInput("Mystery label - can't be seen ?")
        .labelVisibility(JtComponent.LabelVisibility.HIDDEN)
        .use();
    if (text7 != null && !text7.isEmpty()) {
      Jt
          .text("The label was hidden for this one. But the corresponding space was still here ! Today you are %s!".formatted(
              text7))
          .key("t7")
          .use();
    }


    String text6 = Jt
        .textInput("Mystery label - can't be seen ?")
        .labelVisibility(JtComponent.LabelVisibility.COLLAPSED)
        .use();
    if (text6 != null && !text6.isEmpty()) {
      Jt
          .text("The label was hidden for this one, and no corresponding space for this one ! Today you are %s!".formatted(
              text6))
          .key("t6")
          .use();
    }
  }
}
