package com.storygen.convert;

import com.storygen.dto.HtmlDocument;
import org.springframework.stereotype.Component;

@Component
public class HtmlDocumentBuilder {

    public String build(HtmlDocument doc) {
        String escapedTitle = escapeHtml(doc.title());

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                </head>
                <body>
                  <article>
                    <h1>%s</h1>
                %s  </article>
                </body>
                </html>
                """.formatted(escapedTitle, escapedTitle, indentBody(doc.bodyHtml()));
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    private String indentBody(String bodyHtml) {
        String[] lines = bodyHtml.split("\\n", -1);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (!line.isBlank()) {
                sb.append("    ").append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
