[![progress-banner](https://backend.codecrafters.io/progress/http-server/3744f915-817b-49f4-946b-d79dd0a0c680)](https://app.codecrafters.io/users/codecrafters-bot?r=2qF)

This is a starting point for Java solutions to the
["Build Your Own HTTP server" Challenge](https://app.codecrafters.io/courses/http-server/overview).

[HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) is the
protocol that powers the web. In this challenge, you'll build a HTTP/1.1 server
that is capable of serving multiple clients.

Along the way you'll learn about TCP servers,
[HTTP request syntax](https://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html),
and more.

**Note**: If you're viewing this repo on GitHub, head over to
[codecrafters.io](https://codecrafters.io) to try the challenge.

# Passing the first stage

The entry point for your HTTP server implementation is in
`src/main/java/Main.java`. Study and uncomment the relevant code, and push your
changes to pass the first stage:

```sh
git commit -am "pass 1st stage" # any msg
git push origin master
```

Time to move on to the next stage!

# Stage 2 & beyond

Note: This section is for stages 2 and beyond.

1. Ensure you have `mvn` installed locally
1. Run `./your_program.sh` to run your program, which is implemented in
   `src/main/java/Main.java`.
1. Commit your changes and run `git push origin master` to submit your solution
   to CodeCrafters. Test output will be streamed to your terminal.

# HTTP Server Implementation

A robust HTTP/1.1 server implementation in Java that supports multiple concurrent clients and various HTTP features.

## Features

- HTTP/1.1 compliant
- Multi-threaded server supporting concurrent connections
- Support for keep-alive connections
- GZIP compression support
- File operations (GET/POST)
- Echo endpoint
- User-Agent endpoint
- Proper error handling
- Configurable port and directory

## Project Structure

```
src/main/java/com/http/server/
├── HttpServer.java           # Main server class
├── handler/
│   ├── ConnectionHandler.java # Handles client connections
│   └── RequestHandler.java    # Processes HTTP requests
├── request/
│   ├── HttpRequest.java      # HTTP request model
│   └── RequestParser.java    # Parses HTTP requests
└── response/
    ├── HttpResponse.java     # HTTP response model
    └── ResponseBuilder.java  # Builds and sends responses
```

## Prerequisites

- Java 17 or higher
- Maven 3.6.0 or higher

## Building the Project

```bash
mvn clean package
```

This will create an executable JAR file at `target/codecrafters-http-server.jar`.

## Running the Server

Basic usage:
```bash
java -jar target/codecrafters-http-server.jar
```

With custom port and directory:
```bash
java -jar target/codecrafters-http-server.jar 8080 --directory=/path/to/files
```

### Command Line Arguments

- `--directory=<path>`: Set the directory for file operations
- `<port>`: Set the server port (default: 4221)

## API Endpoints

### Root Endpoint
```
GET /
```
Returns: 200 OK with empty body

### Echo Endpoint
```
GET /echo/<text>
```
Returns: 200 OK with the echoed text

### User-Agent Endpoint
```
GET /user-agent
```
Returns: 200 OK with the client's User-Agent header

### File Operations

#### GET File
```
GET /files/<filename>
```
Returns: 
- 200 OK with file contents if file exists
- 404 Not Found if file doesn't exist

#### POST File
```
POST /files/<filename>
```
Returns:
- 201 Created if file was created successfully
- 500 Internal Server Error if file creation failed

## Example Usage

1. Start the server:
```bash
java -jar target/codecrafters-http-server.jar --directory=.
```

2. Test endpoints:
```bash
# Root endpoint
curl http://localhost:4221/

# Echo endpoint
curl http://localhost:4221/echo/hello

# User-Agent endpoint
curl -H "User-Agent: test-agent" http://localhost:4221/user-agent

# File operations
# Create a file
curl -X POST -d "Hello, World!" http://localhost:4221/files/test.txt

# Get a file
curl http://localhost:4221/files/test.txt
```

## Error Handling

The server handles various error conditions:
- 400 Bad Request: Invalid request format
- 404 Not Found: Requested resource not found
- 405 Method Not Allowed: Unsupported HTTP method
- 500 Internal Server Error: Server-side errors

## Keep-Alive Connections

The server supports keep-alive connections by default. To close a connection, send:
```
Connection: close
```

## GZIP Compression

The server supports GZIP compression when requested by the client:
```
Accept-Encoding: gzip
```

## License

This project is part of the [CodeCrafters](https://codecrafters.io) HTTP Server Challenge.
