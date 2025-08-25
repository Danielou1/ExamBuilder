# ExamBuilder

## Description

ExamBuilder is a desktop application for creating and managing exams. It allows users to create questions, organize them into exams, and export them to various formats such as DOCX and JSON.

## Features

*   Create and manage questions with different types (e.g., multiple choice, open-ended).
*   Organize questions into exams.
*   Export exams to DOCX, PDF and JSON formats.
*   Rephrase questions using an integrated tool.

## Technologies Used

*   Java 19
*   JavaFX 19
*   Maven
*   Apache POI for DOCX export
*   iText for PDF export
*   Jackson for JSON serialization
*   Log4j2 for logging

## Installation

1.  Clone the repository:
    ```bash
    git clone https://github.com/Danielou1/ExamBuilder.git
    ```
2.  Navigate to the project directory:
    ```bash
    cd ExamBuilder
    ```
3.  Build the project using Maven:
    ```bash
    mvn clean install
    ```

## Usage

Run the application using the following command:

```bash
mvn javafx:run
```

Or, you can run the packaged JAR file:

```bash
java -jar target/exambuilder-1.0-SNAPSHOT.jar
```

## Configuration

To use the question rephrasing feature, you need to provide a Gemini API key.

### Option 1: Environment Variable (Recommended)

Set the `GEMINI_API_KEY` environment variable to your API key.

*   **Windows:**
    ```bash
    setx GEMINI_API_KEY "YOUR_API_KEY"
    ```
*   **macOS/Linux:**
    ```bash
    export GEMINI_API_KEY="YOUR_API_KEY"
    ```

### Option 2: Hardcode the API Key (For local testing only)

1.  Open the file `src/main/java/utils/Rephraser.java`.
2.  Uncomment the following line:
    ```java
    // return "YOUR_API_KEY";
    ```
3.  Replace `"YOUR_API_KEY"` with your actual API key.

**IMPORTANT:** Do not commit this file with the API key uncommented.

## Author

*   **Danielou Mounsande**
*   **Email:** mounsandedaniel@gmail.com
*   **GitHub:** [github.com/Danielou1](https://github.com/Danielou1)

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.
