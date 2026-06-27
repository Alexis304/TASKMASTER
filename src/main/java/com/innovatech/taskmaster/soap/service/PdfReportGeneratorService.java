package com.innovatech.taskmaster.soap.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

@Service
public class PdfReportGeneratorService {

    public byte[] generarPdf(String titulo, String generadoPor, String fechaGeneracion, String contenido) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setLeading(15);
                contentStream.newLineAtOffset(50, 735);

                writeLine(contentStream, PDType1Font.HELVETICA_BOLD, 16, titulo);
                contentStream.newLine();
                writeLine(contentStream, PDType1Font.HELVETICA, 10, "Generado por: " + generadoPor);
                contentStream.newLine();
                writeLine(contentStream, PDType1Font.HELVETICA, 10, "Fecha: " + fechaGeneracion);
                contentStream.newLine();
                contentStream.newLine();

                contentStream.setFont(PDType1Font.HELVETICA, 10);
                for (String line : wrapContent(contenido, 92)) {
                    contentStream.showText(sanitize(line));
                    contentStream.newLine();
                }

                contentStream.endText();
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el PDF del tablero.", exception);
        }
    }

    private void writeLine(PDPageContentStream contentStream, PDType1Font font, int size, String text) throws IOException {
        contentStream.setFont(font, size);
        contentStream.showText(sanitize(text));
    }

    private List<String> wrapContent(String content, int maxLength) {
        List<String> lines = new ArrayList<>();
        String[] paragraphs = String.valueOf(content).split("\\R", -1);

        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                lines.add("");
                continue;
            }

            String currentLine = "";
            for (String word : paragraph.split("\\s+")) {
                if ((currentLine + " " + word).trim().length() > maxLength) {
                    lines.add(currentLine);
                    currentLine = word;
                } else {
                    currentLine = (currentLine + " " + word).trim();
                }
            }
            if (!currentLine.isBlank()) {
                lines.add(currentLine);
            }
        }

        return lines;
    }

    private String sanitize(String value) {
        return String.valueOf(value)
            .replace('\t', ' ')
            .replace('\r', ' ')
            .replace('\n', ' ');
    }
}
