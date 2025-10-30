

import io.javelit.core.Jt;

public class HelloWorld {

    public static void main(String[] args) {
        Jt.title("Welcome to Javelit! \uD83D\uDEA1").use();
        Jt.markdown("Javelit is an open-source Java app framework built specifically for fast app development.   "
                    + "Build your next data app, back-office, internal tool, or demo with Javelit!").use();
        Jt.markdown("""
                            ## Want to learn more?
                            - Check out [javelit.io](https://javelit.io)
                            - Jump into our [documentation](https://docs.javelit.io)
                            - Ask a question in the [community forum](https://github.com/javelit/javelit/discussions)
                            
                            ## Start building
                            - Create [your first app](https://docs.javelit.io/get-started/tutorials/create-an-app)!
                            - Create [a multipage app](https://docs.javelit.io/get-started/tutorials/create-a-multipage-app)!
                            
                            ## Start tinkering directly in this app! 
                            A file `HelloWorld.java` was just created with the content of this app.   
                            Open this file and add the following to the `main` method:
                            ```java
                            Jt.markdown("# Hello World!").use(); 
                            ```
                            Save your file. You will see the update right in this app!
                            
                            You can then add interactivity.
                            Here's a checkbox: 
                            ```java
                            boolean showTable = Jt.checkbox("Show table").use();
                            if (showTable) {
                              Jt.table(List.of(Map.of("Name", "Javelit", "age", 1), Map.of("Name", "Streamlit", "age", 6))).use();
                            }
                            ```
                            """).use();
    }
}
