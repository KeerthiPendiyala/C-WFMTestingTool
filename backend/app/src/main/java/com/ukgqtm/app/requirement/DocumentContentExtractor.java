package com.ukgqtm.app.requirement;

import com.ukgqtm.app.config.OpenAiProperties;
import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.Set;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DocumentContentExtractor {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("PDF", "DOCX", "DOC", "CSV");
    private static final long MAX_FILE_SIZE = 25L * 1024L * 1024L;
    private final int maxExtractedCharacters;

    public DocumentContentExtractor(OpenAiProperties properties) {
        maxExtractedCharacters = properties.getMaxExtractedCharacters();
    }

    public ExtractedDocument extract(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().trim();
        String extension = extension(filename);
        if (file.isEmpty()) {
            throw invalid("Select a non-empty document.");
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw invalid("Upload a PDF, DOCX, DOC or CSV document.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw invalid("The document must be 25 MB or smaller.");
        }

        try {
            byte[] bytes = file.getBytes();
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);
            BodyContentHandler handler = new BodyContentHandler(maxExtractedCharacters);
            new AutoDetectParser().parse(new ByteArrayInputStream(bytes), handler, metadata, new ParseContext());
            String content = handler.toString().trim();
            if (content.isBlank()) {
                throw new RequirementGenerationException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "No readable text was found in the uploaded document.");
            }
            return new ExtractedDocument(filename, extension, content, bytes);
        } catch (RequirementGenerationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RequirementGenerationException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "The document could not be read. Confirm that it is not encrypted or corrupted.",
                    exception);
        }
    }

    private static String extension(String filename) {
        int separator = filename.lastIndexOf('.');
        return separator < 0 ? "" : filename.substring(separator + 1).toUpperCase(Locale.ROOT);
    }

    private static RequirementGenerationException invalid(String message) {
        return new RequirementGenerationException(HttpStatus.BAD_REQUEST, message);
    }

    public record ExtractedDocument(String filename, String extension, String content, byte[] bytes) {}
}
