

## Working on examples:
- Make sure you have the Jbang plugin installed in your IDE
- Mark the examples directory as sources to get IDE completion and highlighting
  - In IntelliJ IDE
    1. Right-click on the examples folder
    2. Mark directory as --> Sources Root
- Open an example. Jbang should be detected. 
  1. Open an example
  2. Right click anywhere in the file --> Sync JBang DEPS to Module

## Build the doc
```bash
./mvnw clean package -Prelease -DskipTests
```
