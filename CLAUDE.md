# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

**Jeamlit** is a Streamlit-like framework for building data apps in Java.

- **Type**: Java application
- **Build System**: Maven
- **Java Version**: 23 (configured in pom.xml, runs on Java 21+)
- **Package Structure**: `tech.catheu`
- **Design Decisions**: See DESIGN_DECISIONS.md

## Build Commands

### Building
```bash
./mvnw clean compile    # Clean and compile
./mvnw package          # Package as JAR
./mvnw clean            # Clean build directory
```

### Running
TODO

### Testing
```bash
./mvnw test                        # Run all tests
./mvnw test -Dtest=TestClassName   # Run specific test
./mvnw package -DskipTests         # Build without tests
```

## Project Structure

```
src/main/java/tech/catheu/  # Main application code
src/test/java/              # Unit tests
src/main/resources/         # Application resources
```

## Configuration

**pom.xml** - Maven configuration
- Group ID: `tech.catheu`
- Artifact ID: `jeamlit`
- Version: `1.0-SNAPSHOT`

## Development Notes

- Maven wrapper (mvnw) included - no global Maven installation needed
- Standard Maven directory structure
- See DESIGN_DECISIONS.md for architectural details
- ignore TODO.md
- all examples and test apps should be put in the examples folder

### Implementation of Components
- Do not create or update methods in Jt,  like Jt.button(), Jt.theNewComponent(). I will do this later. 
- Focus on the quality of the component themselves.
- Make sure to look at streamlit doc. I will share the links. Make sure to also look at it during implementation.
- Make sure to build a proper plans for each component, especially if they are complex. It's ok if the task becomes very big.
- always use a builder pattern like it's done in the existing components
- use the same color effects and animations as streamlit components
- Ensure things compile regularly
- always use lit. To defined components
- when implementing frontend components: do not re-create window.jeamlit and a function to send messages via websocket. Just assume window.jeamlit.emit is available (thanks to index.html) 
- again make sure to follow the same spec as streamlit
- There is 1 exception to this: do not add args/kwargs equivalent (varargs and Map parameters). Just put a comment instead, saying it's not implemented.
- For a component that will have to support use_container_width:
    - Main containers (:host in lit css) should always be in display block
    - Then :host([use-container-width]) .[REPLACE_BY_SOME_INTERNAL_CLASS] { width: 100%; }
- Main containers (:host in lit css) should always be in display block 