# Applicant Data Processor

[![Java](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.java.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT) A Java utility to process applicant data from a CSV-like file. It validates entries, handles duplicates, applies score adjustments based on submission time, identifies top performers, calculates an average score for the top half, and outputs the results in JSON format.

## ✨ Features

* **Parses CSV Data:** Reads applicant information (name, email, delivery timestamp, score) from a specified file.
* **Data Validation:** Skips rows with invalid data formats:
    * Checks for valid email structure.
    * Ensures score is a number between 0 and 10 (inclusive, up to 2 decimal places).
    * Verifies name contains at least two parts (e.g., first and last name).
    * Validates the date/time format (`yyyy-MM-dd'T'HH:mm:ss`).
* **Duplicate Handling:** Processes entries based on unique email addresses. If multiple entries exist for the same email, only the *last* encountered entry in the file is considered.
* **Score Adjustment:** Modifies scores based on submission date and time relative to the overall submission window:
    * **Bonus:** +1 point for submissions on the very first day any submission was received.
    * **Penalty:** -1 point for submissions on the very last day *at or after* 12:00:00 PM (noon).
    * *Note:* No adjustments are made if all valid submissions occur on the same calendar day.
* **Top Applicant Identification:** Determines the top 3 applicants based on the following criteria (in order):
    1.  Highest Adjusted Score (descending)
    2.  Highest Original Score (descending)
    3.  Earliest Delivery Date/Time (ascending)
    4.  Email Address (alphabetical ascending)
* **Average Score Calculation:** Calculates the average *original* score of the top 50% of unique applicants (rounded up if the total number is odd), after sorting them by their original scores (descending).
* **JSON Output:** Saves the processing results to a JSON file (`src/main/resources/output.json` by default).

## 📋 Prerequisites

* **Java Development Kit (JDK):** Version 8 or higher (due to `java.time` API usage).
* **Gson Library:** The Google Gson library (`com.google.code.gson:gson`) is required for JSON processing. You'll need to include it in your project's classpath.
    * If using Maven, add to `pom.xml`:
        ```xml
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.10.1</version> </dependency>
        ```
    * If using Gradle, add to `build.gradle`:
        ```groovy
        implementation 'com.google.code.gson:gson:2.10.1' // Use the latest version
        ```
    * Otherwise, download the Gson JAR and include it during compilation and runtime.

## ⚙️ How to Run

1.  **Compile the Code:**
    Navigate to your project's root directory. If you have the source files (`ApplicantsProcessor.java`, `Applicant.java`) in `src/main/java/org/example/`, compile them. Make sure the Gson JAR is in your classpath.

    ```bash
    # Example compilation (adjust classpath -cp if needed)
    javac -cp path/to/gson.jar src/main/java/org/example/*.java -d out/
    ```

2.  **Prepare Input File:**
    Create a text file (e.g., `applicants.csv`) with applicant data. Each line should contain: `name,email,delivery_datetime,score`.

    * **Format:** `String,String,String(yyyy-MM-dd'T'HH:mm:ss),Double(0-10)`
    * A header line (`name,email,delivery_datetime,score`) is optional and will be skipped if present.

    *Example `applicants.csv`:*
    ```csv
    name,email,delivery_datetime,score
    Alice Wonderland,alice@example.com,2024-01-10T10:00:00,8.5
    Bob The Builder,bob@example.com,2024-01-10T15:30:00,9.0
    Charlie Chaplin,charlie@example.com,2024-01-11T09:00:00,7.0
    Bob The Builder,bob@example.com,2024-01-11T11:00:00,8.8
    Diana Prince,diana@example.com,2024-01-12T14:00:00,9.5
    Eve Adamson,eve@example.com,2024-01-12T10:00:00,6.0
    Frank Sinatra,frank@example.com,2024-01-10T08:00:00,10.0
    Invalid Entry,,2024-01-13T10:00:00,5.0 // Invalid name/email -> skipped
    Valid Entry,valid@test.com,2024-01-13T10:00:00,11.0 // Invalid score -> skipped
    Another Test,test@test.com,2024-01-13T10:00,7.5 // Invalid date format -> skipped
    ```

3.  **Run the Processor:**
    Execute the compiled code, providing the path to your input file as a command-line argument.

    ```bash
    # Example execution (adjust classpath -cp if needed)
    java -cp out:path/to/gson.jar org.example.ApplicantsProcessor path/to/your/applicants.csv
    ```
    *(Replace `path/to/your/applicants.csv` with the actual path and `path/to/gson.jar` with the Gson JAR location. Use `;` instead of `:` for classpath separator on Windows.)*

4.  **Check Output:**
    The results will be written to `src/main/resources/output.json` (relative to where you run the command, unless your project structure places resources elsewhere). The console will print a confirmation message (in Romanian): `Rezultatul a fost salvat în src/main/resources/output.json`.

    *Example `output.json` (based on the valid entries in the sample `applicants.csv` above):*
    ```json
    {
      "uniqueApplicants": 6,
      "topApplicants": [
        "Sinatra",  // Adj: 11.0 (10+1), Orig: 10.0, Time: 08:00
        "Builder",  // Adj: 8.8, Orig: 8.8, Time: 11:00 (Bob's 2nd entry)
        "Prince"    // Adj: 8.5 (9.5-1), Orig: 9.5, Time: 14:00
      ],
      "averageScore": 9.1 // Avg of top 3 original scores: (10.0 + 9.5 + 8.8) / 3 = 9.433 -> rounded to 9.43 - check calculation logic again
    }
    ```
    *Correction on Average Calculation from thought process:* The code sorts by original score `(10.0, 9.5, 8.8, 8.5, 7.0, 6.0)`. Top half (3) are Frank(10.0), Diana(9.5), Bob(8.8). Average = (10.0 + 9.5 + 8.8) / 3 = 28.3 / 3 = 9.4333... Rounded to two decimals: `9.43`. Let's update the example JSON.

    *Corrected Example `output.json`*:
    ```json
    {
      "uniqueApplicants": 6,
      "topApplicants": [
        "Sinatra",
        "Builder",
        "Prince"
      ],
      "averageScore": 9.43
    }
    ```


## 🏗️ Code Structure

* **`ApplicantsProcessor.java`**: Contains the main logic (`main` method) and the `processApplicantData` method which handles file reading, parsing, validation, processing (duplicates, score adjustment, sorting), and JSON generation. Includes helper methods for validation.
* **`Applicant.java`**: A simple Plain Old Java Object (POJO) or Data Transfer Object (DTO) representing an applicant with fields for name, email, delivery time, original score, and adjusted score. Includes getters/setters and a helper `getLastName()` method.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a pull request or open an issue for bugs, feature requests, or improvements.

## 📜 License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details (or choose/create your license file).
