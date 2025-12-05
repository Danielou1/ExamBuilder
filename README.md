# ExamBuilder

[![Java CI with Maven](https://github.com/Danielou1/ExamBuilder/actions/workflows/build.yml/badge.svg)](https://github.com/Danielou1/ExamBuilder/actions/workflows/build.yml)

## Conception and Implementation of a Software Solution for the Automated Creation and Administration of Exams

ExamBuilder is a modern desktop application designed for educators to streamline the creation, management, and export of exams. Developed in JavaFX as a bachelor thesis project, it focuses on solving the practical challenges teachers face by providing a robust alternative to traditional word processors.

---

### Screenshots

*Add screenshots of your application here to visually showcase its interface.*

**Main Window:**
![Main Window Screenshot](Bachelorarbeit/img/screenshot_main.png)

**Question Editor:**
![Question Editor Screenshot](Bachelorarbeit/img/screenshot_editor.png)

---

## Core Features

#### Exam Project Management
- **Create, Save, Load:** Manage complete exam projects in `JSON` format, with smart prompts to prevent losing unsaved changes.
- **Robust Import:** Easily import existing `JSON` files, even from older versions of the application.

#### Question Authoring
- **Rich Text Editor (HTML):** Format question text (bold, italic, underline) and create bulleted or numbered lists with an integrated HTML editor.
- **Image Support:** Embed images directly into questions. They are previewed in the UI and included in the Word export.
- **Varied Question Types:** Supports Multiple Choice Questions (MCQ), Open-Ended Questions, and Fill-in-the-Blank (`Lückentext`).
- **Hierarchical Structure:** Organize exams into exercises and questions, with the ability to add nested sub-questions.

#### Export and Customization
- **Export to Microsoft Word (`.docx`):** Generate professional and well-formatted exam documents and answer keys.
- **Selective Content:** Precisely choose which questions to include in the final export using a dedicated selection mode.
- **Controlled Layout:** Use the "New Page" option to insert page breaks before main questions, ensuring a clean layout.
- **Automated Answer Key:** Generate a separate answer sheet (`Musterlösung`) that includes both textual and visual solutions.
- **Question Shuffling:** Shuffle the order of questions within an exercise to create exam variations.
- **Question Rephrasing:** Alter the wording of questions by replacing words with synonyms (based on a local dictionary, no AI).

#### Modern User Interface
- **Light & Dark Themes:** Switch between two themes for better visual comfort.
- **German UI:** The entire user interface is designed in German for local relevance.
- **Intuitive Design:** Enjoy icons, tooltips, and context menus for a smooth and efficient workflow.

---

## Tech Stack

- **Language:** Java 19
- **UI Framework:** JavaFX 19
- **Build Management:** Maven
- **Continuous Integration:** GitHub Actions

#### Core Libraries
- **Apache POI `5.2.2`:** For generating `.docx` (Microsoft Word) files.
- **Jackson `2.13.3`:** For `JSON` data serialization and deserialization.
- **Jsoup `1.15.3`:** For parsing and cleaning HTML content from the rich text editor.
- **ControlsFX `11.1.2`:** For advanced UI components like autocomplete fields.
- **FontAwesomeFX `4.7.0-9.1.2`:** For icon integration.

#### Testing
- **JUnit 5 `5.8.2`**

---

## Getting Started

### Prerequisites
- JDK (Java Development Kit) - Version 19 or higher.
- Apache Maven (recommended via the included Maven Wrapper).

### Building
To build the project and install dependencies, run the following command from the root directory:

```bash
# On Windows
.\mvnw.cmd clean install

# On macOS/Linux
./mvnw clean install
```

### Running
After building, launch the application with the command:

```bash
# On Windows
.\mvnw.cmd javafx:run

# On macOS/Linux
./mvnw javafx:run
```
You can use the provided `Klausur.json` file (File -> Import from JSON) to test the application with sample data.

---

## Author

- **Danielou Mounsande**
- **Email:** mounsandedaniel@gmail.com
- **GitHub:** [github.com/Danielou1](https://github.com/Danielou1)
- **LinkedIn:** [linkedin.com/in/danielou-mounsande](https://www.linkedin.com/in/danielou-mounsande)

---

## License
This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.
