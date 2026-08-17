package com.ukgqtm.app.requirement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ukgqtm.app.config.OpenAiProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
        assertThat(extracted.sourceType()).isEqualTo("CSV");
        assertThat(extracted.contentType()).isEqualTo("text/csv");
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

        assertThat(extracted.sourceType()).isEqualTo("DOCX");
        assertThat(extracted.content()).contains("Employees can clock in from an approved device.");
    }

    @Test
    void extractsLegacyXlsContent() throws IOException {
        var file = new MockMultipartFile(
                "document",
                "requirements.xls",
                "application/vnd.ms-excel",
                minimalXls("Employees can submit a corrected timesheet."));

        var extracted = extractor.extract(file);

        assertThat(extracted.sourceType()).isEqualTo("XLS");
        assertThat(extracted.contentType()).isEqualTo("application/vnd.ms-excel");
        assertThat(extracted.content()).contains("Employees can submit a corrected timesheet.");
    }

    @Test
    void extractsXlsxContentAsAnOtherReadableFormat() throws IOException {
        var file = new MockMultipartFile(
                "document",
                "requirements.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                minimalXlsx("Managers can approve corrected timesheets."));

        var extracted = extractor.extract(file);

        assertThat(extracted.sourceType()).isEqualTo("OTHER");
        assertThat(extracted.contentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(extracted.content()).contains("Managers can approve corrected timesheets.");
    }

    @Test
    void extractsReadableContentFromAnUnlistedFileExtension() {
        var file = new MockMultipartFile(
                "document",
                "requirements.custom",
                "application/octet-stream",
                "Employees can review their submitted hours.".getBytes(StandardCharsets.UTF_8));

        var extracted = extractor.extract(file);

        assertThat(extracted.sourceType()).isEqualTo("OTHER");
        assertThat(extracted.content()).contains("Employees can review their submitted hours.");
    }

    @Test
    void rejectsFilesWithoutReadableText() {
        var file = new MockMultipartFile(
                "document", "requirements.bin", "application/octet-stream", new byte[] {0, 0, 0, 0});

        assertThatThrownBy(() -> extractor.extract(file))
                .isInstanceOf(RequirementGenerationException.class)
                .hasMessageContaining("No readable text");
    }

    private static byte[] minimalXls(String text) throws IOException {
        var output = new ByteArrayOutputStream();
        try (var workbook = new HSSFWorkbook()) {
            workbook.createSheet("Requirements").createRow(0).createCell(0).setCellValue(text);
            workbook.write(output);
        }
        return output.toByteArray();
    }

    private static byte[] minimalXlsx(String text) throws IOException {
        var output = new ByteArrayOutputStream();
        try (var workbook = new XSSFWorkbook()) {
            workbook.createSheet("Requirements").createRow(0).createCell(0).setCellValue(text);
            workbook.write(output);
        }
        return output.toByteArray();
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
