# Developer developer developer 

This section may lack crucial details. Don't hesitate to reach out on the [forum](https://github.com/javelit/javelit/discussions)!

## Common operations
*In commands below, `clean` is optional*.

**Compile**:
```
./mvnw clean compile
```

**Test**:
```
./mvnw clean test
```

**Build the jars**:
```
./mvnw clean install -DskipTests
```

**Build the doc**:
```bash
./mvnw clean package -Prelease -DskipTests
```

**Check new dependencies**:
```bash
./mvnw versions:display-dependency-updates
```


## Working on examples:
- Make sure you have the Jbang plugin installed in your IDE
- Mark the examples directory as sources to get IDE completion and highlighting
  - In IntelliJ IDE
    1. Right-click on the examples folder
    2. Mark directory as --> Sources Root
- Open an example. Jbang should be detected. 
  1. Open an example
  2. Right click anywhere in the file --> Sync JBang DEPS to Module

## Working on doc examples
The [Jt.java](src/main/java/io/javelit/core/Jt.java) class javadoc uses code snippets.
The snippets should be written in the [snippetFiles](snippetFiles) folder.
Examples should be deployed in railway, in the _docs_ project.
```bash
railway link -p docs
```

To deploy a new app:
Make sure the app in the [snippetFiles](snippetFiles) folder is pushed on GitHub.
Deploy on Railway:
```bash
# change this value with the correct one
APP_NAME=TextExample.java
railway deploy -t javelit-app -v "APP_URL=https://github.com/javelit/javelit/blob/main/snippetFiles/${APP_NAME}"
```

**Make sure to set the service as serverless in the UI afterwards**, then update the URL in the javadoc.

_NOTE: for the moment it is a mix of manual and CLI operations because the railway CLI does not support all the necessary operations_

The javadoc should then be written:
```javadoc
/**
* Write text without Markdown or HTML parsing.
* For monospace text, use {@link Jt#code}
* Examples:
* {@snippet file="TextExample.java" appUrl="<RAILWAY URL>" appHeight="300"}
*
* @param body The string to display.
*/
```

the recommended minimum appHeight is 250. (it is not enforced though) 

#### Resources
List snippets that don't have their link set yet
```bash
FILES=$(grep -Eo '\{@snippet file="[^"]+\.java" appUrl="TODO"' "src/main/java/io/javelit/core/Jt.java" | sed 's/{@snippet file="\([^"]*\)".*/\1/')
```

Deploy a list of names:
```bash
for APP_NAME in $FILES; do   railway deploy -t javelit-app     -v "APP_URL=https://github.com/javelit/javelit/blob/main/snippetFiles/${APP_NAME}";    sleep 3; done
```
