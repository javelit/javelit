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