package com.company.releasenote.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseNoteParserTest {

    private final ReleaseNoteParser parser =
            new ReleaseNoteParser();

    @Test
    void shouldParseDescriptionAndConfiguration() {
        String releaseNote = """
                Description:
                Customer tablosuna status alanı eklenmiştir.

                Configuration:
                ALTER TABLE Customer ADD status VARCHAR(20);

                Components:
                Customer Service
                """;

        ParsedReleaseNote result = parser.parse(releaseNote);

        assertTrue(result.descriptionSectionPresent());
        assertTrue(result.configurationSectionPresent());

        assertEquals(
                "Customer tablosuna status alanı eklenmiştir.",
                result.description()
        );

        assertEquals(
                "ALTER TABLE Customer ADD status VARCHAR(20);",
                result.configuration()
        );

        assertFalse(result.isDescriptionBlank());
        assertFalse(result.isConfigurationBlank());
    }

    @Test
    void shouldDetectEmptyConfigurationSection() {
        String releaseNote = """
            Description:
            Product tablosuna stock_status kolonu eklenmiştir.

            Configuration:
            """;

        ParsedReleaseNote result = parser.parse(releaseNote);

        assertTrue(result.descriptionSectionPresent());
        assertTrue(result.configurationSectionPresent());

        assertFalse(result.isDescriptionBlank());
        assertTrue(result.isConfigurationBlank());
    }

    @Test
    void shouldDetectMissingConfigurationSection() {
        String releaseNote = """
            Description:
            Product tablosuna stock_status kolonu eklenmiştir.

            Components:
            Product Service
            """;

        ParsedReleaseNote result = parser.parse(releaseNote);

        assertTrue(result.descriptionSectionPresent());
        assertFalse(result.configurationSectionPresent());

        assertFalse(result.isDescriptionBlank());
        assertTrue(result.isConfigurationBlank());
    }
}