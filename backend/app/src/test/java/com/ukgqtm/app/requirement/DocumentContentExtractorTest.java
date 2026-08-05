package com.ukgqtm.app.requirement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ukgqtm.app.config.OpenAiProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class DocumentContentExtractorTest {
    private final DocumentContentExtractor extractor = new DocumentContentExtractor(new OpenAiProperties());

    @Test
    void extractsCsvContentWithoutSendingTheFileOutsideTheBackend() {
        var file = new MockMultipartFile(
                "document",
                "requirements.csv",
                "text/csv",
                "Header,Description\nClock in,Capture employee time".getBytes());

        var extracted = extractor.extract(file);

        assertThat(extracted.filename()).isEqualTo("requirements.csv");
        assertThat(extracted.extension()).isEqualTo("CSV");
        assertThat(extracted.content()).contains("Clock in", "Capture employee time");
    }

    @Test
    void extractsDocxContentWithCompatibleArchiveDependencies() throws IOException {
        var file = new MockMultipartFile(
                "document",
                "requirements.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                minimalDocx("Employees can clock in from an approved device."));

        var extracted = extractor.extract(file);

        assertThat(extracted.extension()).isEqualTo("DOCX");
        assertThat(extracted.content()).contains("Employees can clock in from an approved device.");
    }

    @Test
    void rejectsUnsupportedFilesBeforeExtraction() {
        var file = new MockMultipartFile("document", "requirements.exe", "application/octet-stream", new byte[] {1});

        assertThatThrownBy(() -> extractor.extract(file))
                .isInstanceOf(RequirementGenerationException.class)
                .hasMessageContaining("PDF, DOCX, DOC or CSV");
    }

    private static byte[] minimalDocx(String text) throws IOException {
        var output = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            addEntry(
                    zip,
                    "[Content_Types].xml",
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                    </Types>
                    """);
            addEntry(
                    zip,
                    "_rels/.rels",
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                    </Relationships>
                    """);
            addEntry(
                    zip,
                    "word/document.xml",
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                      <w:body><w:p><w:r><w:t>%s</w:t></w:r></w:p></w:body>
                    </w:document>
                    """.formatted(text));
        }
        return output.toByteArray();
    }

    private static void addEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
