import io.javelit.core.Jt;

///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS io.javelit:javelit:0.64.0

public class FormExample {

    public static void main(String[] args) {

        var cols = Jt.columns(2).use();
        Jt.empty().height(15).border(false).use(cols.col(0));
        Jt.markdown("## Outside a form").use(cols.col(0));
        var textInput = Jt.textInput("Outside text input").use(cols.col(0));
        var textArea =  Jt.textArea("Outside text area").use(cols.col(0));
        Jt.empty().height(35).border(false).use(cols.col(0));
        Jt.divider("divider").use(cols.col(0));
        Jt.markdown("## Values from outside").use(cols.col(0));
        Jt.markdown("Text input: " + textInput).use(cols.col(0));
        Jt.markdown("Text area: " + textArea).use(cols.col(0));

        var formContainer = Jt.form().use(cols.col(1));
        Jt.markdown("## Inside a form").use(formContainer);
        var textInputFromForm = Jt.textInput("Inside text input").use(formContainer);
        var textAreaFromForm = Jt.textArea("Inside text area").use(formContainer);
        Jt.formSubmitButton("Submit form").use(formContainer);
        Jt.markdown("## Values from inside").use(cols.col(1));
        Jt.markdown("Text input: " + textInputFromForm).key("1").use(cols.col(1));
        Jt.markdown("Text area: " + textAreaFromForm).key("2").use(cols.col(1));
    }
}
