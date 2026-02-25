# AptaSuite Backend

A Spring Boot backend for web-based aptamer bioinformatics analysis. This is a modern rewrite of the original AptaSuite, designed to provide REST APIs for analyzing HT-SELEX (High-Throughput Systematic Evolution of Ligands by Exponential Enrichment) experiments.

## About

AptaSuite Backend reimplements the core functionality of the original AptaSuite platform for the web. It processes sequencing data from aptamer selection experiments, providing tools for sequence analysis, structural predictions, and experiment management.

**Note**: This is an ongoing rewrite and does not yet have complete feature parity with the original AptaSuite application.

## Requirements

- Java 25 or higher
- PostgreSQL 18+ (production only; the `dev` profile uses embedded H2)

## Installation

1. Clone the repository:

   ```bash
   git clone <repository-url>
   cd aptasuite
   ```

2. Choose how to run the app:

   - **Local development (no DB startup required):**

     ```bash
     ./gradlew bootRun --args='--spring.profiles.active=dev'
     ```

     This uses an embedded H2 database persisted at `./.data/aptasuite.mv.db`.

   - **Production-like local run with PostgreSQL:**

     Create a `.secrets.yml` file in the project root with PostgreSQL connection settings:

     ```yaml
     POSTGRES_URL: jdbc:postgresql://localhost:5432/aptasuite
     POSTGRES_USER: username
     POSTGRES_PASSWORD: password
     ```

3. Build the project:

   ```bash
   ./gradlew build
   ```

4. Run the application:

   ```bash
   ./gradlew bootRun
   ```

The server will start on `http://localhost:8080`.

## Configuration

The main configuration file is located at `src/main/resources/application.yml`. Key settings include:

- Maximum file upload size (default: 100MB)
- PostgreSQL datasource and JPA settings (via `.secrets.yml`)

## Credits

This project is a derivative work of **AptaSUITE**:

> **AptaSUITE: A Full-Featured Bioinformatics Framework for the Comprehensive Analysis of Aptamers from HT-SELEX Experiments**  
> Hoinka, J., Backofen, R., & Przytycka, T. M. (2018)  
> *Molecular Therapy – Nucleic Acids*, 11, 515–517  

[![GitHub](https://img.shields.io/badge/GitHub-drivenbyentropy/AptaSUITE-6f42c1)](https://github.com/drivenbyentropy/aptasuite)
[![DOI](https://img.shields.io/badge/DOI-10.1016%2Fj.omtn.2018.04.006-blue)](https://doi.org/10.1016/j.omtn.2018.04.006)
