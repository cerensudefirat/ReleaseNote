package com.company.releasenote.parser;

public record ParsedReleaseNote(
        String description,
        String configuration,
        boolean descriptionSectionPresent,
        boolean configurationSectionPresent
) {
    public ParsedReleaseNote{
        description=description==null
                ? "" : description.trim();

        configuration=configuration==null ? "" : configuration.trim();
    }
    public boolean isDescriptionBlank(){
        return description.isBlank();
    }
    public boolean isConfigurationBlank(){
        return configuration.isBlank();
    }
}
