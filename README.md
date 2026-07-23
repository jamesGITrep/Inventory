# Inventory Management System

A robust Inventory Management System built with Java and Spring Boot.

## Technology Stack

* **Core Language**: Java 21
* **Backend Framework**: Spring Boot 3.4.4
  * Spring Data JPA
  * Spring Boot Actuator
* **Database**: MySQL
* **Frontend**: JavaFX 21
  * ControlsFX 11.1.2
* **Build Tool**: Maven

## Getting Started

### Prerequisites

* Java Development Kit (JDK) 21 or higher
* Maven 3.x
* MySQL Server

### Database Setup

1. Create a MySQL database for the application.
2. Update the `src/main/resources/application.properties` file with your database credentials.

### Running the Application

You can run the application using Maven:

```bash
./mvnw spring-boot:run
```

Or using the JavaFX plugin:

```bash
./mvnw javafx:run
```

## Features
- Inventory tracking
- Product management
- Stock level monitoring
- MySQL database integration
