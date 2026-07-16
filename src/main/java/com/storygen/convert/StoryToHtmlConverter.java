package com.storygen.convert;

import com.storygen.dto.HtmlDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class StoryToHtmlConverter {

    private static final Pattern SCENE_BREAK = Pattern.compile("^\\s*(?:\\*{3}|-{3}|#{3})\\s*$");
    private static final Pattern BLANK_LINES = Pattern.compile("\\n\\s*\\n");

    public HtmlDocument convert(String rawStory) {
        if (rawStory == null || rawStory.isBlank()) {
            return new HtmlDocument("Untitled Story", "<p></p>");
        }

        String[] lines = rawStory.split("\\n", -1);

        String title;
        int bodyStartIndex;

        if (lines.length > 0 && lines[0].trim().length() <= 120 && !lines[0].trim().isEmpty()) {
            title = escapeHtml(lines[0].trim());
            bodyStartIndex = 1;
        } else {
            title = "Untitled Story";
            bodyStartIndex = 0;
        }

        StringBuilder bodyText = new StringBuilder();
        for (int i = bodyStartIndex; i < lines.length; i++) {
            if (i > bodyStartIndex) {
                bodyText.append("\n");
            }
            bodyText.append(lines[i]);
        }

        String trimmedBody = bodyText.toString().trim();
        if (trimmedBody.isEmpty()) {
            return new HtmlDocument(title, "<p></p>");
        }

        String[] blocks = BLANK_LINES.split(trimmedBody, -1);
        List<String> elements = new ArrayList<>();

        for (String block : blocks) {
            String trimmedBlock = block.trim();
            if (trimmedBlock.isEmpty()) {
                continue;
            }

            if (isSceneBreak(trimmedBlock)) {
                elements.add("<hr class=\"scene-break\">");
            } else if (isDialogue(trimmedBlock)) {
                elements.add("<p class=\"dialogue\">" + escapeHtml(trimmedBlock) + "</p>");
            } else {
                elements.add("<p>" + escapeHtml(trimmedBlock) + "</p>");
            }
        }

        String bodyHtml = String.join("\n", elements);
        return new HtmlDocument(title, bodyHtml);
    }

    private boolean isSceneBreak(String text) {
        return SCENE_BREAK.matcher(text).matches();
    }

    private boolean isDialogue(String text) {
        String trimmed = text.strip();
        if (trimmed.length() < 2) {
            return false;
        }
        return trimmed.startsWith("\u201C") && trimmed.endsWith("\u201D")
                || trimmed.startsWith("\"") && trimmed.endsWith("\"");
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }
}
