package com.company.releasenote.client;

import com.company.releasenote.config.OllamaProperties;
import com.company.releasenote.dto.llm.CoverageAnalysis;
import com.company.releasenote.dto.llm.DescriptionAnalysis;
import com.company.releasenote.dto.llm.ollama.OllamaChatMessage;
import com.company.releasenote.dto.llm.ollama.OllamaChatRequest;
import com.company.releasenote.dto.llm.ollama.OllamaChatResponse;
import com.company.releasenote.exception.InvalidLlmResponseException;
import com.company.releasenote.exception.LlmServiceException;
import com.company.releasenote.model.ChangeCategory;
import com.company.releasenote.model.Coverage;
import com.company.releasenote.model.Requirement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class OllamaLlmClient implements LlmClient {

    private static final String DESCRIPTION_SYSTEM_PROMPT = """
            Sen bir release note Description analiz asistanısın.

            Yalnızca verilen Description metnine dayan.
            Configuration bölümünü düşünme.
            Metinde olmayan teknik ayrıntıları varsayma.
            Eksik bilgi üretme.
            Yalnızca istenen JSON sonucunu üret.

            REQUIREMENT:
            - Configuration gerektiren açık veya güçlü bir değişiklik varsa REQUIRED.
            - Yalnızca UI görünümü, metin veya kod içi değişiklik varsa NOT_REQUIRED.
            - Description gerçekten anlaşılamıyorsa UNCERTAIN.

            CATEGORY:
            - Tablo, kolon, index, constraint, sequence veya veri değişikliği: DATABASE.
            - Dışarıdan değiştirilebilen timeout, retry count, cache duration veya feature flag:
              APPLICATION_PROPERTY.
            - Environment variable: ENVIRONMENT_VARIABLE.
            - Servis URL'si, endpoint veya harici bağlantı: EXTERNAL_SERVICE.
            - Kafka topic, queue, exchange veya consumer group: MESSAGING.
            - Docker, Kubernetes, port, volume veya deployment ayarı: DEPLOYMENT.
            - Dosya veya dizin yolu değişikliği: FILE_OR_DIRECTORY.
            - Sertifika, keystore, truststore, secret veya güvenlik ayarı:
              SECURITY_OR_CERTIFICATE.
            - Cron veya zamanlanmış görev değişikliği: SCHEDULED_JOB.
            - UI metni, renk veya görünüm: UI_CHANGE.
            - Yalnızca kod içi değişiklik: CODE_CHANGE.
            - Kategori belirlenemiyorsa UNKNOWN.
            """;

    private static final String COVERAGE_SYSTEM_PROMPT = """
        Sen bir release note Configuration coverage analiz asistanısın.

        Requirement ve category daha önce belirlenmiştir.
        Requirement veya category değerini yeniden değerlendirme.
        Yalnızca Configuration bölümünün Description bölümündeki
        değişiklik için yeterli bilgi içerip içermediğini belirle.

        Yalnızca şu dört değerden birini seç:
        - SUFFICIENT
        - INCOMPLETE
        - UNRELATED
        - UNCERTAIN

        KARAR SIRASI:

        1. Configuration, Description'daki değişiklikten farklı bir konudan
           bahsediyorsa UNRELATED seç.

        2. Configuration aynı konudan bahsediyor ancak değişikliğin nasıl
           uygulanacağına ilişkin somut bilgi vermiyorsa INCOMPLETE seç.

        3. Yalnızca somut uygulama bilgisi varsa SUFFICIENT seç.

        4. Gerçekten karar verilemiyorsa UNCERTAIN seç.

        ÖNEMLİ:
        Configuration'ın yalnızca doğru konu veya kategoriyle ilgili olması
        SUFFICIENT seçmek için yeterli değildir.

        DATABASE KURALLARI:

        DATABASE kategorisinde SUFFICIENT seçmek için Configuration içinde
        aşağıdaki bilgilerden en az biri bulunmalıdır:

        - Gerçek bir SQL, DDL veya DML komutu
        - Migration dosyası, migration adı veya migration sürümü
        - Uygulanacak tablo, kolon, index veya constraint adıyla birlikte
          açık bir işlem
        - Gerçekleştirilecek somut veri güncelleme veya backfill işlemi

        Aşağıdaki gibi yalnızca genel ifadeler INCOMPLETE kabul edilmelidir:

        - Database changes are required.
        - Required database changes must be applied.
        - Database configuration should be updated.
        - Migration should be applied.
        - Veritabanı değişiklikleri uygulanmalıdır.
        - Gerekli database işlemleri yapılmalıdır.

        Bu ifadeler veritabanıyla ilgilidir fakat neyin ve nasıl
        uygulanacağını açıklamadığı için SUFFICIENT değildir.

        DATABASE ÖRNEKLERİ:

        Description:
        Product tablosuna stock_status kolonu eklenmiştir.

        Configuration:
        Required database changes must be applied.

        Sonuç:
        INCOMPLETE

        Description:
        Product tablosuna stock_status kolonu eklenmiştir.

        Configuration:
        ALTER TABLE Product ADD stock_status VARCHAR(20);

        Sonuç:
        SUFFICIENT

        Description:
        Product tablosuna stock_status kolonu eklenmiştir.

        Configuration:
        Application port değeri 8081 yapılmalıdır.

        Sonuç:
        UNRELATED

        APPLICATION_PROPERTY KURALLARI:

        SUFFICIENT için property adı, configuration key'i, environment
        değişkeni veya değiştirilecek somut değer bulunmalıdır.

        "Application configuration should be updated" gibi yalnızca genel
        ifadeler INCOMPLETE kabul edilmelidir.

        Teknik doğruluk kontrolü yapma.
        SQL'in veya property değerinin doğru olup olmadığını denetleme.
        Eksik SQL, property veya migration bilgisi üretme.
        Yalnızca istenen JSON sonucunu üret.
        """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OllamaProperties properties;

    public OllamaLlmClient(
            @Qualifier("ollamaRestClient")
            RestClient restClient,
            ObjectMapper objectMapper,
            OllamaProperties properties
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public DescriptionAnalysis analyzeDescription(
            String description
    ) {
        OllamaChatRequest request = new OllamaChatRequest(
                properties.model(),
                List.of(
                        new OllamaChatMessage(
                                "system",
                                DESCRIPTION_SYSTEM_PROMPT
                        ),
                        new OllamaChatMessage(
                                "user",
                                "Description:\n" + description)
                ),
                false,
                false,
                createDescriptionSchema(),
                createOptions()
        );

        DescriptionAnalysis result = executeRequest(
                request,
                DescriptionAnalysis.class
        );

        if (result.requirement() == null
                || result.category() == null) {
            throw new InvalidLlmResponseException(
                    "Description analiz sonucu eksik alan içeriyor."
            );
        }

        return result;
    }

    @Override
    public CoverageAnalysis analyzeCoverage(
            ChangeCategory category,
            String description,
            String configuration
    ) {
        String userPrompt = """
        Daha önce belirlenmiş category:
        %s

        Description:
        ---
        %s
        ---

        Configuration:
        ---
        %s
        ---

        Yalnızca Configuration coverage değerini belirle.

        Hatırlatma:
        Aynı konudan bahsetmek tek başına SUFFICIENT değildir.
        Somut uygulama detayı yoksa INCOMPLETE seç.
        """.formatted(
                category.name(),
                description,
                configuration
        );

        OllamaChatRequest request = new OllamaChatRequest(
                properties.model(),
                List.of(
                        new OllamaChatMessage(
                                "system",
                                COVERAGE_SYSTEM_PROMPT

                        ),
                        new OllamaChatMessage(
                                "user",
                                userPrompt

                        )
                ),
                false,
                false,
                createCoverageSchema(),
                createOptions()
        );

        CoverageAnalysis result = executeRequest(
                request,
                CoverageAnalysis.class
        );

        if (result.coverage() == null) {
            throw new IllegalStateException(
                    "Coverage analiz sonucu eksik alan içeriyor."
            );
        }

        return result;
    }

    private <T> T executeRequest(
            OllamaChatRequest request,
            Class<T> resultType
    ) {
        String rawResponse;

        try {

            rawResponse = restClient
                    .post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);

        } catch (RestClientResponseException exception) {
            throw new LlmServiceException(
                    "Ollama HTTP hatası döndürdü: "
                            + exception.getStatusCode(),
                    exception
            );

        } catch (RestClientException exception) {
            throw new LlmServiceException(
                    "Ollama servisine ulaşılamadı.",
                    exception
            );

        } catch (JacksonException exception) {
            throw new InvalidLlmResponseException(
                    "Ollama isteği JSON'a dönüştürülemedi.",
                    exception
            );
        }

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalStateException(
                    "Ollama tamamen boş bir HTTP cevabı döndürdü."
            );
        }

        OllamaChatResponse response;

        try {
            response = objectMapper.readValue(
                    rawResponse,
                    OllamaChatResponse.class
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Ollama dış cevap JSON'u okunamadı.",
                    exception
            );
        }

        validateResponse(response);

        try {
            return objectMapper.readValue(
                    response.message().content(),
                    resultType
            );

        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Ollama geçerli bir analiz JSON'u döndürmedi. Gelen içerik: "
                            + response.message().content(),
                    exception
            );
        }
    }
    private void validateResponse(
            OllamaChatResponse response
    ) {
        if (response == null) {
            throw new InvalidLlmResponseException(
                    "Ollama boş cevap döndürdü."
            );
        }

        if (!response.done()) {
            throw new InvalidLlmResponseException(
                    "Ollama cevabı tamamlanmadı."
            );
        }

        if (response.message() == null) {
            throw new InvalidLlmResponseException(
                    "Ollama cevabında message alanı bulunamadı."
            );
        }

        if (response.message().content() == null
                || response.message().content().isBlank()) {

            throw new InvalidLlmResponseException(
                    "Ollama cevap içeriği boş."
                            + " doneReason=" + response.doneReason()
            );
        }
    }
    private Map<String, Object> createDescriptionSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "requirement", Map.of(
                                "type", "string",
                                "enum", enumNames(Requirement.class)
                        ),
                        "category", Map.of(
                                "type", "string",
                                "enum", enumNames(ChangeCategory.class)
                        )
                ),
                "required", List.of(
                        "requirement",
                        "category"
                ),
                "additionalProperties", false
        );
    }

    private Map<String, Object> createCoverageSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "coverage", Map.of(
                                "type", "string",
                                "enum", enumNames(Coverage.class)
                        )
                ),
                "required", List.of("coverage"),
                "additionalProperties", false
        );
    }

    private Map<String, Object> createOptions() {
        return Map.of(
                "temperature",
                properties.temperature(),

                "num_predict",
                properties.maxOutputTokens()
        );
    }

    private <E extends Enum<E>> List<String> enumNames(
            Class<E> enumType
    ) {
        return Arrays.stream(enumType.getEnumConstants())
                .map(Enum::name)
                .toList();
    }
}