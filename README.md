# Battleships online game, Web application (Backend)
[Link to the Frontend](https://github.com/LukasChyle/battleship-frontend)

## Content
- [About](#about)
- [Abstract](#abstract)
- [Tools](#tools)
- [Example images](#example-images)

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

## Tools (used in backend)
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

### Lombok
A Java library that reduces boilerplate code by generating getter/setter methods, constructors, and more at compile time. Improves code readability but adds compile-time dependencies that can affect debugging.

### Log4j2
A popular Java-based logging framework used for capturing, storing, and managing log messages in applications.

## Example images
![Screenshot 2024-05-23 080858](https://github.com/user-attachments/assets/31048333-926e-4270-95ce-b341214fe5e7)

![Screenshot 2024-05-15 171721](https://github.com/user-attachments/assets/aa85f5ea-ac36-45ea-a5b0-873f27ac6219)

![Screenshot 2024-05-19 142559](https://github.com/user-attachments/assets/19cf4fc3-24a6-47d0-a5dc-ada63719886d)

![Screenshot 2024-05-15 172537](https://github.com/user-attachments/assets/a95a3e7d-e391-4700-8cea-c3ec4d3e9e9b)

![Screenshot 2024-05-15 173303](https://github.com/user-attachments/assets/441d5598-6d8d-46b6-8b3e-1edaf19db5dc)

![Screenshot 2024-05-15 185908](https://github.com/user-attachments/assets/e66d4b8c-1778-4912-893d-7c8f72477b97)

![Screenshot 2024-05-15 175143](https://github.com/user-attachments/assets/781819e7-9b38-43d8-ad4c-41c2f067edc9)

![Screenshot 2024-05-15 161648](https://github.com/user-attachments/assets/3f27b7a5-4cc5-4544-beb9-98299e3a03fd)

![Screenshot 2024-05-15 174954](https://github.com/user-attachments/assets/77ff88c2-f0b6-450e-9fb7-a71e0c758ba6)
