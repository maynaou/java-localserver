# Java Local Server

A lightweight HTTP/1.1-compliant web server built in Java using non-blocking I/O (NIO). No external frameworks — only Java core libraries.

---

## Project Structure

```
java-server/
├── pom.xml
├── README.md
└── src/
    └── main/
        └── java/
            └── com/
                └── javaserver/
                    ├── Main.java                  # Entry point
                    ├── config/
                    │   └── ServerConfig.java      # Server configuration model
                    ├── http/
                    │   ├── HttpRequest.java        # Parsed HTTP request object
                    │   └── HttpRequestParser.java  # HTTP request parser
                    └── server/
                        └── Server.java            # Core server (NIO event loop)
```

---

## Requirements

- Java 17 or higher
- Maven 3.6 or higher

Check if installed:
```bash
java -version
mvn -version
```

Install if missing:
```bash
# Ubuntu/Debian
sudo apt install default-jdk maven

# macOS (Homebrew)
brew install openjdk maven
```

---

## How to Run

### 1. Clone or download the project
```bash
cd java-server
```

### 2. Compile
```bash
mvn compile
```

### 3. Run
```bash
mvn exec:java -Dexec.mainClass="com.javaserver.Main"
```

### 4. Or build a JAR and run it
```bash
mvn package
java -jar target/java-server-1.0-SNAPSHOT.jar
```

---