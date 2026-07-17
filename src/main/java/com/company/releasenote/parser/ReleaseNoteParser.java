package com.company.releasenote.parser;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ReleaseNoteParser {

    /*
     * Release note içerisinde bulunabilecek bilinen başlıklar.
     *
     * Bu başlıklar, Description veya Configuration bölümünün
     * nerede bittiğini anlamak için kullanılır.
     */
    private static final String ALL_SECTION_HEADINGS =
            "(?:"
                    + "Description"
                    + "|Açıklama"
                    + "|Configuration"
                    + "|Konfigürasyon"
                    + "|Yapılandırma"
                    + "|Components"
                    + "|Bileşenler"
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
                    | Pattern.DOTALL
                    | Pattern.UNICODE_CASE;

    /*
     * Description başlığının bulunup bulunmadığını kontrol eder.
     */
    private static final Pattern DESCRIPTION_HEADING_PATTERN =
            Pattern.compile(
                    "^\\s*(?:Description|Açıklama)\\s*:",
                    PATTERN_FLAGS
            );

    /*
     * Configuration başlığının bulunup bulunmadığını kontrol eder.
     */
    private static final Pattern CONFIGURATION_HEADING_PATTERN =
            Pattern.compile(
                    "^\\s*(?:Configuration|Konfigürasyon|Yapılandırma)\\s*:",
                    PATTERN_FLAGS
            );

    /*
     * Description başlığından başlayıp bir sonraki bilinen
     * başlığa kadar olan metni alır.
     */
    private static final Pattern DESCRIPTION_SECTION_PATTERN =
            Pattern.compile(
                    "^\\s*(?:Description|Açıklama)\\s*:\\s*"
                            + "(.*?)"
                            + "(?=^\\s*"
                            + ALL_SECTION_HEADINGS
                            + "\\s*:|\\z)",
                    PATTERN_FLAGS
            );

    /*
     * Configuration başlığından başlayıp bir sonraki bilinen
     * başlığa kadar olan metni alır.
     */
    private static final Pattern CONFIGURATION_SECTION_PATTERN =
            Pattern.compile(
                    "^\\s*(?:Configuration|Konfigürasyon|Yapılandırma)\\s*:\\s*"
                            + "(.*?)"
                            + "(?=^\\s*"
                            + ALL_SECTION_HEADINGS
                            + "\\s*:|\\z)",
                    PATTERN_FLAGS
            );

    public ParsedReleaseNote parse(String releaseNote) {
        String normalizedText = normalize(releaseNote);

        boolean descriptionSectionPresent =
                DESCRIPTION_HEADING_PATTERN
                        .matcher(normalizedText)
                        .find();

        boolean configurationSectionPresent =
                CONFIGURATION_HEADING_PATTERN
                        .matcher(normalizedText)
                        .find();

        String description = extractSection(
                DESCRIPTION_SECTION_PATTERN,
                normalizedText
        );

        String configuration = extractSection(
                CONFIGURATION_SECTION_PATTERN,
                normalizedText
        );

        return new ParsedReleaseNote(
                description,
                configuration,
                descriptionSectionPresent,
                configurationSectionPresent
        );
    }

    private String extractSection(
            Pattern sectionPattern,
            String releaseNote
    ) {
        Matcher matcher = sectionPattern.matcher(releaseNote);

        if (!matcher.find()) {
            return "";
        }

        return matcher.group(1).trim();
    }

    private String normalize(String releaseNote) {
        if (releaseNote == null) {
            return "";
        }

        return releaseNote
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim();
    }
}