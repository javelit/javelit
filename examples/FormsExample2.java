/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.81.0

import io.javelit.core.Jt;

public class FormsExample2 {

  public static void main(String[] args) throws InterruptedException {
    Jt.title("Forms Example").use();
    Jt.text("This example demonstrates form functionality with batching.").use();

    var form = Jt.form()
                 .clearOnSubmit(false)
                 .border(true)
                 .use();

    var name = Jt.textInput("Enter your name").use(form);
    var age = Jt.numberInput("age", Integer.class).minValue(0).maxValue(100).value(25).use(form);
    //var email = Jt.textInput("email", "Enter email").use(form);

    if (Jt.formSubmitButton("Submit").use(form)) {
      Jt.title("Form Submitted!").use();
      Jt.text("Name: " + name).use();
      Jt.text("Age: " + age).use();
    }

    Jt.text("Content outside the form updates immediately:").use();
    var counter = Jt.slider("counter").min(0).max(10).value(0).use();
    Jt.text("Counter value: " + counter).use();
  }
}
