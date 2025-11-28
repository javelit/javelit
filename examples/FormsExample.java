/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.75.0

import io.javelit.core.Jt;

public class FormsExample {

  public static void main(String[] args) throws InterruptedException {
    Jt.title("Forms Example").use();
    Jt.text("This example demonstrates form functionality with batching.").use();

    var form = Jt.form()
                 .clearOnSubmit(false)
                 .border(true)
                 .use();

    //var name = Jt.textInput("name", "Enter your name").use(form);
    var age = Jt.slider("age").min(0).max(100).value(25).use(form);
    //var email = Jt.textInput("email", "Enter email").use(form);

    if (Jt.formSubmitButton("Submit")
          .onClick(e -> Jt.text("I WAS CLICKED!").use())
          .use(form)) {
      // All form values are now committed in a single batch
      Jt.title("Form Submitted!").use();
      //Jt.text("Name: " + name).use();
      Jt.text("Age: " + age).use();
      //Jt.text("Email: " + email).use();
    }

    Jt.text("age is " + age).use();

    Jt.text("Content outside the form updates immediately:").use();
    var counter = Jt.slider("counter").min(0).max(10).value(0).use();
    Jt.text("Counter value: " + counter).use();
  }
}
