# AptaSuite Backend

A Spring Boot backend for web-based aptamer bioinformatics analysis. This is a modern rewrite of the original AptaSuite, designed to provide REST APIs for analyzing HT-SELEX (High-Throughput Systematic Evolution of Ligands by Exponential Enrichment) experiments.

## About

AptaSuite Backend reimplements the core functionality of the original AptaSuite platform for the web. It processes sequencing data from aptamer selection experiments, providing tools for sequence analysis, structural predictions, and experiment management.

**Note**: This is an ongoing rewrite and does not yet have complete feature parity with the original AptaSuite application.

## Requirements

- Java 25 or higher
- MongoDB (local instance or cloud-based like MongoDB Atlas)

## Installation

1. Clone the repository:

   ```bash
   git clone <repository-url>
   cd aptasuite
   ```

2. Create a `.secrets.yml` file in the project root with your MongoDB connection string:

   ```yaml
   MONGODB_URI: mongodb+srv://username:password@cluster.mongodb.net/aptasuite
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
- MongoDB connection (via `.secrets.yml`)

## Credits

This project is a derivative work of **AptaSUITE**:

> **AptaSUITE: A Full-Featured Bioinformatics Framework for the Comprehensive Analysis of Aptamers from HT-SELEX Experiments**  
> Hoinka, J., Backofen, R., & Przytycka, T. M. (2018)  
> *Molecular Therapy – Nucleic Acids*, 11, 515–517  

[![GitHub](https://img.shields.io/badge/GitHub-drivenbyentropy/AptaSUITE-6f42c1)](https://github.com/drivenbyentropy/aptasuite)
[![DOI](https://img.shields.io/badge/DOI-10.1016%2Fj.omtn.2018.04.006-blue)](https://doi.org/10.1016/j.omtn.2018.04.006)
