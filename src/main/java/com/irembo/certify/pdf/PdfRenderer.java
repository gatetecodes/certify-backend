package com.irembo.certify.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class PdfRenderer {

    public byte[] renderHtmlToPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String content = html;
            if (html != null && !html.toLowerCase().contains("<html")) {
                content =
                        "<!DOCTYPE html><html><head><meta charset='UTF-8'></meta></head><body>"
                                + html +
                                "</body></html>";
            }

            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(content, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render PDF", e);
        }
    }
}
