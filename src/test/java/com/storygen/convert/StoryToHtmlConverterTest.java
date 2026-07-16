package com.storygen.convert;

import com.storygen.dto.HtmlDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StoryToHtmlConverterTest {

    private StoryToHtmlConverter converter;

    @BeforeEach
    void setUp() {
        converter = new StoryToHtmlConverter();
    }

    @Test
    void normalMultiParagraphStory() {
        String story = """
                The Lighthouse Keeper
                The old lighthouse stood at the edge of the world.
                Waves crashed against the rocks below.

                Every night, Thomas lit the beacon.
                He whispered to the sea, and the sea whispered back.

                Storms came and went.
                The light never went dark.
                """;

        HtmlDocument doc = converter.convert(story);
        assertEquals("The Lighthouse Keeper", doc.title());
        assertTrue(doc.bodyHtml().contains("<p>"));
        assertTrue(doc.bodyHtml().contains("Every night, Thomas lit the beacon."));
        assertFalse(doc.bodyHtml().contains("<h1>"));
        // 3 paragraphs after the title line
        assertEquals(3, countOccurrences(doc.bodyHtml(), "<p>"));
    }

    @Test
    void storyWithDialogue() {
        // Dialogue must be WHOLLY wrapped in double quotes per the spec
        String story = """
                A Conversation at Dawn

                She walked to the edge of the pier.

                "I have been waiting for you."

                The wind carried the words away.
                """;

        HtmlDocument doc = converter.convert(story);
        assertEquals("A Conversation at Dawn", doc.title());
        assertTrue(doc.bodyHtml().contains("<p class=\"dialogue\">"));
        assertTrue(doc.bodyHtml().contains("I have been waiting for you."));
    }

    @Test
    void storyWithSceneBreak() {
        String story = """
                Two Worlds

                The morning sun rose over the valley.

                ***

                Night fell without warning.
                """;

        HtmlDocument doc = converter.convert(story);
        assertEquals("Two Worlds", doc.title());
        assertTrue(doc.bodyHtml().contains("<hr class=\"scene-break\">"));
        assertFalse(doc.bodyHtml().contains("class=\"scene-break\">") == false);
    }

    @Test
    void htmlUnsafeCharactersEscaped() {
        String story = """
                Escape Test

                This has <script>alert('xss')</script> and & characters.

                Also <img src=x onerror=alert(1)> should be safe.
                """;

        HtmlDocument doc = converter.convert(story);
        assertTrue(doc.bodyHtml().contains("&lt;script&gt;"));
        assertFalse(doc.bodyHtml().contains("<script>"));
        assertTrue(doc.bodyHtml().contains("&amp;"));
    }

    @Test
    void singleParagraphNoBlankLines() {
        String story = "Just one line with no breaks at all, a continuous stream of text.";
        HtmlDocument doc = converter.convert(story);
        assertEquals("Just one line with no breaks at all, a continuous stream of text.", doc.title());
        assertTrue(doc.bodyHtml().contains("<p>"));
    }

    @Test
    void emptyStory() {
        HtmlDocument doc = converter.convert("");
        assertEquals("Untitled Story", doc.title());
        assertTrue(doc.bodyHtml().contains("<p>"));
    }

    @Test
    void fallbackTitleWhenFirstLineTooLong() {
        String story = """
                This is a very long first line that exceeds one hundred and twenty characters and should trigger the fallback title mechanism instead of being used as the actual title
                Body text here.
                """;

        HtmlDocument doc = converter.convert(story);
        assertEquals("Untitled Story", doc.title());
        assertTrue(doc.bodyHtml().contains("This is a very long first line"));
    }

    @Test
    void dialogueWithSmartQuotes() {
        // Dialogue must be WHOLLY wrapped in smart quotes per the spec
        String story = """
                Smart Dialogue

                \u201CIs anyone there?\u201D

                Silence was the only answer.
                """;

        HtmlDocument doc = converter.convert(story);
        assertTrue(doc.bodyHtml().contains("<p class=\"dialogue\">"));
    }

    @Test
    void sceneBreakWithDashes() {
        String story = """
                Dash Break

                Part one.

                ---

                Part two.
                """;

        HtmlDocument doc = converter.convert(story);
        assertTrue(doc.bodyHtml().contains("<hr class=\"scene-break\">"));
    }

    @Test
    void sceneBreakWithHashes() {
        String story = """
                Hash Break

                Part one.

                ###

                Part two.
                """;

        HtmlDocument doc = converter.convert(story);
        assertTrue(doc.bodyHtml().contains("<hr class=\"scene-break\">"));
    }

    private int countOccurrences(String text, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
