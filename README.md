# Battleships online game, Web application (Backend)
[Link to the game: https://lukaschyle.github.io/battleship/](https://lukaschyle.github.io/battleship/)

[Link to the frontend repository](https://github.com/LukasChyle/battleship-frontend)

## Content
- [About](#about)
- [Abstract](#abstract)
- [To Do](#to-do)
- [Tools](#tools)

## About
This project was my thesis for a Higher Vocational Education program in Java development.
The application is designed to be scalable and lightweight, which is why it was built without a database.
This reduces complexity while maintaining performance.
Additionally, by not storing sensitive data, the application minimizes security risks.

## Abstract
The objective of this project was to develop a web application facilitating communication
between clients (frontend) and a server (backend) using WebSocket, a communication
protocol technology that enables interactive two-way communication between a client and a
server over a single TCP connection. It facilitates real-time communication between the
client and the server, meaning data can be sent and received instantly without the client
needing to repeat requests.
A good use case for WebSocket is for games that require real-time communication between
players. This project is a web application where users can play Battleship online by
connecting to a server. The server pairs clients into two-player game sessions, enabling
interactive gameplay over the internet.
The server is responsible for managing all game logic, ensuring that clients do not have
access to sensitive information regarding the game session.
The primary focus has been on creating a visually appealing and user-friendly gaming
environment within a scalable system.
The frontend is developed using JavaScript with the React library, while the backend is
implemented in Java utilizing the Spring Boot framework.

## To Do
- Create unit and integration tests
- Create documentation

## Tools
### (used in backend)

### Spring Boot
A Java-based framework for building backend services and web applications. It simplifies dependency management, includes an embedded server (Tomcat, Netty, etc.), and offers production-ready features out of the box.

### WebSocket
A communication protocol enabling bidirectional, real-time data exchange between a client and server over a single persistent TCP connection, reducing the need for repeated requests.

### WebFlux
A reactive alternative to Spring MVC, designed for asynchronous, non-blocking HTTP processing. Best suited for high-throughput applications but requires careful handling of blocking operations like database queries.

### Netty (Server)
A low-level, high-performance networking framework for building scalable, asynchronous applications.

### Spring Boot Starter WebFlux
A module for developing reactive, non-blocking web applications using Spring WebFlux. Supports built-in reactive servers like Netty or Undertow and is optimized for WebSocket handling.

### R2DBC
A reactive, non-blocking alternative to JDBC for accessing relational databases. Designed for use with frameworks like Spring WebFlux to support asynchronous data operations using Mono and Flux.

### MariaDB
An open-source relational database system and MySQL-compatible fork, known for speed, reliability, and active community-driven development.

### Lombok
A Java library that reduces boilerplate code by generating getter/setter methods, constructors, and more at compile time. Improves code readability but adds compile-time dependencies that can affect debugging.

### Log4j2
A popular Java-based logging framework used for capturing, storing, and managing log messages in applications.
