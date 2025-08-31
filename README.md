# ExamBuilder

## Description

ExamBuilder is a modern desktop application designed for educators to streamline the creation, management, and distribution of exams. With an intuitive user interface, it allows for the creation of complex exams, which can be saved, loaded, and exported into multiple standard formats.

The application is built with JavaFX and includes advanced features like question rephrasing via the Gemini API and dynamic theme switching.

## Features

*   **Full Project Lifecycle:** Create, save, and load entire exam projects.
*   **Question Management:** Add, edit, and delete questions within an exam.
*   **Supported Question Types:**
    *   Multiple Choice Questions (MCQ)
    *   Open-Ended Questions
*   **Import/Export:**
    *   **Import** exams from `JSON` files.
    *   **Export** exams to `PDF`, `DOCX`, and `JSON` formats for easy distribution and grading.
*   **AI-Powered Rephrasing:** Integrate with the Gemini API to automatically rephrase questions, offering alternative phrasings.
*   **Customizable UI:** Instantly switch between a **light** and **dark** theme.
*   **User-Friendly:** Enjoy a clean interface with icons (via FontAwesomeFX) and helpful dialogs.

## Technologies Used

*   **Core:** Java 19, JavaFX 19
*   **Build:** Maven
*   **File Formats:**
    *   Apache POI `5.2.2` (for `.docx` Word export)
    *   iText `7.2.6` (for `.pdf` export)
    *   Jackson `2.13.3` (for `.json` serialization)
*   **UI:**
    *   FontAwesomeFX `4.7.0-9.1.2` (for icons)
*   **Logging:** Log4j2 `2.17.1`
*   **Testing:**
    *   JUnit 5 `5.8.2`
    *   Mockito `4.5.1`

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

Run the application using the dedicated Maven plugin:

```bash
mvn javafx:run
```

Alternatively, you can run the packaged JAR file from the `target` directory:

```bash
java -jar target/exambuilder-1.0-SNAPSHOT.jar
```

## Configuration

To use the AI-powered question rephrasing feature, you must provide a Gemini API key.

### Environment Variable (Recommended)

Set the `GEMINI_API_KEY` environment variable to your API key. This is the most secure and recommended method.

*   **Windows:**
    ```bash
    setx GEMINI_API_KEY "YOUR_API_KEY"
    ```
    *(Note: You may need to restart your terminal or IDE for the change to take effect.)*

*   **macOS/Linux:**
    ```bash
    export GEMINI_API_KEY="YOUR_API_KEY"
    ```

## Running Tests

To run the suite of unit tests, use the following Maven command:

```bash
mvn test
```

## Author

*   **Danielou Mounsande**
*   **Email:** mounsandedaniel@gmail.com
*   **GitHub:** [github.com/Danielou1](https://github.com/Danielou1)

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.