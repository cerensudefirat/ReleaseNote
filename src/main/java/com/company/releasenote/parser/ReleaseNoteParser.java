package com.company.releasenote.parser;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ReleaseNoteParser {

    private enum SectionType {
        DESCRIPTION,
        CONFIGURATION,
        OTHER
    }

    private static final String ALL_SECTION_HEADINGS =
            "(?:"
                    + "Description"
                    + "|Açıklama"
                    + "|Configuration"
                    + "|Konfigürasyon"
                    + "|Yapılandırma"
                    + "|Components"
                    + "|Bileşenler"
                    + "|Test\\s+Environment\\s+and\\s+Status"
                    + "|Test\\s+Environment"
                    + "|Test\\s+Ortamı"
                    + "|Test\\s+Status"
                    + "|Test\\s+Durumu"
                    + "|Warning\\s+Notes?"
                    + "|Warnings?"
                    + "|Uyarı\\s+Notları"
                    + "|Uyarılar"
                    + ")";

    private static final int PATTERN_FLAGS =
            Pattern.CASE_INSENSITIVE
                    | Pattern.MULTILINE
                    | Pattern.UNICODE_CASE;

    private static final Pattern SECTION_HEADING_PATTERN =
            Pattern.compile(
                    "^\\s*\\*?\\s*("
                            + ALL_SECTION_HEADINGS
                            + ")\\s*\\*?\\s*:\\s*\\*?\\s*",
                    PATTERN_FLAGS
            );

    public ParsedReleaseNote parse(String releaseNote) {
        String normalizedText = normalizeText(releaseNote);

        Matcher matcher = SECTION_HEADING_PATTERN.matcher(normalizedText);

        String description = "";
        String configuration = "";

        boolean descriptionSectionPresent = false;
        boolean configurationSectionPresent = false;

        SectionMatch currentSection = null;

        while (matcher.find()) {
            if (currentSection != null) {
                String sectionContent = normalizedText
                        .substring(currentSection.contentStart(), matcher.start())
                        .trim();

                if (currentSection.sectionType() == SectionType.DESCRIPTION) {
                    description = sectionContent;
                } else if (currentSection.sectionType() == SectionType.CONFIGURATION) {
                    configuration = sectionContent;
                }
            }

            String heading = matcher.group(1);
            SectionType sectionType = resolveSectionType(heading);

            if (sectionType == SectionType.DESCRIPTION) {
                descriptionSectionPresent = true;
            } else if (sectionType == SectionType.CONFIGURATION) {
                configurationSectionPresent = true;
            }

            currentSection = new SectionMatch(sectionType, matcher.end());
        }

        if (currentSection != null) {
            String sectionContent = normalizedText
                    .substring(currentSection.contentStart())
                    .trim();

            if (currentSection.sectionType() == SectionType.DESCRIPTION) {
                description = sectionContent;
            } else if (currentSection.sectionType() == SectionType.CONFIGURATION) {
                configuration = sectionContent;
            }
        }

        return new ParsedReleaseNote(
                description,
                configuration,
                descriptionSectionPresent,
                configurationSectionPresent
        );
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n");
    }

    private SectionType resolveSectionType(String heading) {
        String normalizedHeading = heading
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();

        return switch (normalizedHeading) {
            case "description", "açıklama" -> SectionType.DESCRIPTION;

            case "configuration", "konfigürasyon", "yapılandırma" ->
                    SectionType.CONFIGURATION;

            default -> SectionType.OTHER;
        };
    }

    private record SectionMatch(
            SectionType sectionType,
            int contentStart
    ) {
    }
}