# Jeamlit <span style="transform: scale(-1,1); display:inline-block;">🚡</span>

Welcome! 
## What is Jeamlit ? 
Jeamlit is a **Java** *lightning fast* data app development framework, heavily inspired by (*drumroll…* 🥁) Streamlit!  

Jeamlit makes it dead simple to create data apps **in minutes**. 
Build dashboards, back-offices, generate reports, showcase APIS, etc...   
**The best part?** You can run your Jeamlit app standalone, or **embed it right into your existing Java project**.

<img src="images/demo.gif" alt="Streamlit Hello" height=300 href="none"></img>

*Not convinced? It's ok. Read the [Oh No! Another high-level framework](#oh-no-another-high-level-framework) and [Shouldn't I use Streamlit?](#shouldnt-i-use-streamlit) sections.*

- [Install](#install)
- [Get Started]()
- [Documentation](https://docs.jeamlit.io/) 
- [Forum](https://github.com/jeamlit/jeamlit/discussions/)

## Install

Jeamlit requires A Java JDK >= `21`.

There are 2 main ways to install and run Jeamlit:
- as a **standalone** CLI and app runner
- **embedded** in an existing Java project

You'll find a short version below. [Read the doc](https://docs.jeamlit.io/get-started/installation) to get more details for each method.


### Standalone
1. Install the CLI ([JBang](https://www.jbang.dev/) is highly recommended)
    ```bash
    # recommended: install with jbang
    jbang app install io.jeamlit:jeamlit:0.40.0:all

    # vanilla
    curl -L -o jeamlit.jar https://repo1.maven.org/maven2/io/jeamlit/jeamlit/0.40.0/jeamlit-0.40.0-all.jar
    ```
2. Validate the installation by running the Hello app:
   ```bash
   # jbang
   jeamlit hello
   
   # vanilla
   java -jar jeamlit.jar hello
   ```
3. Play with the Hello World!
5. Want to see a fancier app ? 
   ```bash
   jeamlit run https://raw.githubusercontent.com/jeamlit/jeamlit/main/examples/getting_started/App.java
   ```

Find more details in the [standalone installation doc](https://docs.jeamlit.io/get-started/installation/standalone). 
**Don't forget to install the [JBang IDE plugin](https://docs.jeamlit.io/get-started/installation/standalone#prerequisites) for completion and highlighting!** 

Once you're ready to go further, look at the [fundamental concepts](https://docs.jeamlit.io/get-started/fundamentals) or jump straight into [creating your first app](https://docs.jeamlit.io/get-started/tutorials/create-an-app). 

### Embedded server
1. Add the dependency to your project
   ```xml
   <dependency>
       <groupId>io.jeamlit</groupId>
       <artifactId>jeamlit</artifactId>
       <version>0.40.0</version>
   </dependency>
   ```
2. Launch the server in your project
   ```java
   void startJeamlitServer() {
    // the Jeamlit webapp class
    class MyApp {
      public static void main(String[] args) {
        Jt.text("Hello World").use();
        }
      }
    
      // prepare a Jeamlit server
      var server = Server.builder(MyApp.class, 8888).build();
    
     // start the server - this is non-blocking, user thread
     server.start();
   }
   ```

Find more details in the [embedded installation doc](https://docs.jeamlit.io/get-started/installation/embedded-vanilla#development-with-hot-reload).
**Don't forget to look at the [IDE hot-reload setup](https://docs.jeamlit.io/get-started/installation/embedded-vanilla#development-with-hot-reload)!**

Once you're ready to go further, look at the [fundamental concepts](https://docs.jeamlit.io/get-started/fundamentals) or jump straight into [creating your first app](https://docs.jeamlit.io/get-started/tutorials/create-an-app).


## Quickstart
Create a new file named App.java in your project directory with the following code:
```java
import io.jeamlit.core.Jt;

public class App { 
    public static void main(String[] args) {
        int x = Jt.slider("Select a value").use();
        Jt.write(x + " squared is " + (x * x)).use();
      }
}
```

Run it:
```
jeamlit run App.java
```

Want more ?
look at the [fundamental concepts](https://docs.jeamlit.io/get-started/fundamentals), jump straight into [creating your first app](https://docs.jeamlit.io/get-started/tutorials/create-an-app).   
Not ambitious enough? Create a LangChain4J AI [multipage app](https://docs.jeamlit.io/get-started/tutorials/create-a-multipage-app). 

## Oh, No! Another high-level framework
Jeamlit is **not** another abstraction layer that hides the HTML/CSS/Javascript. 
That alone is not enough to significantly improve productivity. 
The real pain is in **bindings**: handling events, reacting to changes, passing messages, 
parsing results... you get it.
 

**Jeamlit promise is to remove all of that.**

Here is an example:
```
double size = Jt.slider("How tall are you ? in cm").max(220).use();
if (size > 200) {
    Jt.text("Damn, that huge!").use();
}
```

That's it. That's the **full** webapp code. You can move the slider: the `size` variable 
will take the latest value in the frontend.   
What's the order of execution then ? Top-to-bottom. Every time something happens in the app, 
the app logic re-runs, top-to-bottom, with
the latest values from the frontend.

We hope this sparks your curiosity!

*By the way, once you have jeamlit installed, you can run this example with*
```bash
jeamlit run https://raw.githubusercontent.com/jeamlit/jeamlit/refs/heads/main/examples/readme/App.java 
```

## Shouldn't I use Streamlit?
Jeamlit: 
- is Java-native
- can be embedded directly into your existing Java system. 

If neither of those points matters to you... well, that's farewell. You should use [Streamlit](https://streamlit.io/).   
If you're still there: thanks. There are also plenty of small differences that make Jeamlit worth a try: simpler state management, 
easier custom components, etc...

## Contribute
Thanks for you interest in improving Jeamlit! <span style="transform: scale(-1,1); display:inline-block;">🚡</span>  
To start a discussion, open an [issue](https://github.com/jeamlit/jeamlit/issues) or a thread in the [forum](https://github.com/jeamlit/jeamlit/discussions).   
For development, see [DEVELOPMENT.md](DEVELOPMENT.md).

## License
Jeamlit is free and open-source, licensed under the [Apache 2.0 license](LICENSE).
