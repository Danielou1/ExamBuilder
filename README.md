# ExamBuilder

[![Java CI with Maven](https://github.com/Danielou1/ExamBuilder/actions/workflows/build.yml/badge.svg)](https://github.com/Danielou1/ExamBuilder/actions/workflows/build.yml)

## Conception, Design and Implementation of a Software Solution for the Automated Creation and Administration of Exams

ExamBuilder is a modern desktop application designed for educators to streamline the creation, management, and distribution of exams. With an intuitive user interface, it allows for the creation of complex exams, which can be saved, loaded, and exported into multiple standard formats.

## Screenshots

| Main Window | Instructions Dialog (Link) |
| :---: | :---: |
| ![Main Window (PDF)](img/exam.pdf) | [Instructions Dialog (PDF)](img/hinweise.pdf) |

## Key Features

*   **Exam Project Management:** Create, save, and load entire exam projects in JSON format.
*   **Advanced Question Management:** Add, edit, and delete complex questions with nested sub-questions.
*   **Image Support:** Add images to questions, which are displayed in the UI and embedded in the final Word document.
*   **Supported Question Types:**
    *   Multiple Choice Questions (MCQ)
    *   Open-Ended Questions
*   **Customizable Instructions:** A dedicated dialog allows for configuring the exam duration and allowed aids (calculator, notes, etc.) by modifying a standard text template.
*   **Selective Export:** A dedicated "Selection Mode" allows for precisely choosing which questions to include in the final document.
*   **Flexible Export Options:**
    *   **Export to `.docx`:** Generates a professional exam in Microsoft Word format.
    *   **Export Answer Key:** Generates a separate document with the solutions.
    *   **Export Varied Version:** Generates a version of the exam where question text is rephrased (using a local synonym dictionary) and the order of sub-questions is shuffled.
*   **Modern User Interface:**
    *   Switch between Light and Dark themes.
    *   Responsive layout that adapts to different screen sizes using a scrollable editing pane.
    *   Icons (via FontAwesomeFX) and tooltips for a better user experience.
    *   Autocomplete field for German university names.
    *   Right-click context menus for quick access to actions.

## Recent Updates

*   **New Exam with Save Prompt:** Added a "New Exam" feature in the File menu. Loading an exam from JSON or making any modification now correctly flags the session as having unsaved changes, ensuring the user is prompted to save their work before creating a new exam or importing another file.
*   **Enhanced HTML Editor Data Binding:** Changes made in the rich text editor are now automatically saved to the question model when the editor loses focus, ensuring all modifications are preserved during export.
*   **Accurate Word Formatting:** Resolved issues with bold, italic, and underline styles not being correctly applied in the generated Word documents.
*   **Improved MCQ Detection:** Refined the detection logic for multiple-choice options in Word export to prevent unintended checkbox insertions within regular text.
*   **Reduced Spacing in Word Export (MCQ):** Significantly reduced excessive vertical spacing between MCQ question titles and options, and between individual options, in the generated Word documents for a cleaner layout.
*   **Robust Lückentext UI Update:** Implemented a more robust mechanism to prevent 'Lückentext' content from being overwritten by placeholder text during UI updates, ensuring user input persistence.
*   **Clear Fields for Sub-Questions:** Ensured that GUI input fields are automatically cleared when preparing to add a new sub-question, providing a clean slate for new entries.
*   **Resolved Unsaved Changes Dialog Loop:** Fixed an issue causing the 'Unsaved Changes' confirmation dialog to reappear in an infinite loop under certain conditions.
*   **PDF Export Deprecation:** Please note that PDF export is no longer supported; only Word export is available.
*   **UI Language:** The entire application user interface is in German.

## Technologies Used

*   **Core:** Java 19, JavaFX 19
*   **Build:** Maven (with Maven Wrapper)
*   **CI/CD:** GitHub Actions
*   **Core Libraries:**
    *   **Apache POI `5.2.2`** (for `.docx` Word export)
    *   **Jackson `2.13.3`** (for `.json` serialization)
    *   **ControlsFX `11.1.2`** (for autocomplete functionality)
    *   **FontAwesomeFX `4.7.0-9.1.2`** (for icons)
*   **Testing:** JUnit 5 `5.8.2`

## Getting Started

### Prerequisites

*   JDK (Java Development Kit) version 19 or higher.
*   Apache Maven.

### Build

To build the project and install dependencies, run the following command from the root directory:

```bash
# On Windows
.\mvnw.cmd clean install

# On macOS/Linux
./mvnw clean install
```

### Running

After building the project, you can run the application using:

```bash
# On Windows
.\mvnw.cmd javafx:run

# On macOS/Linux
./mvnw javafx:run
```

## Author

*   **Danielou Mounsande**
*   **Email:** mounsandedaniel@gmail.com
*   **GitHub:** [github.com/Danielou1](https://github.com/Danielou1)
*   **LinkedIn:** [linkedin.com/in/danielou-mounsande](https://www.linkedin.com/in/danielou-mounsande)

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.
