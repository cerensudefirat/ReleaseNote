# Release Note Configuration Analyzer

Bu proje, release note dokümanlarında yer alan `Description` ve `Configuration` bölümlerini analiz ederek, Configuration bilgisinin gerekli, eksik, yetersiz, ilgisiz veya yeterli olup olmadığını belirleyen Spring Boot tabanlı bir backend uygulamasıdır.

Uygulama, yerel ortamda Ollama üzerinden çalışan `qwen3:4b-instruct` LLM modelini kullanır. Bu projede yeni bir yapay zekâ modeli eğitilmemiştir; hazır eğitilmiş bir model yerel ortamda çalıştırılmıştır.

## Projenin Amacı

Release note içinde yapılan değişiklikler `Description` bölümünde açıklanır. Eğer bu değişiklik bir veritabanı değişikliği, application property güncellemesi, environment variable, harici servis ayarı veya benzeri bir configuration gerektiriyorsa, detayların `Configuration` bölümünde bulunması beklenir.

Bu proje şu kontrolleri yapar:

* Description bölümü Configuration gerektiriyor mu?
* Configuration bölümü boş mu?
* Configuration bölümü Description ile ilgili mi?
* Configuration bilgisi yeterli mi?

## Sonuç Değerleri

API aşağıdaki sonuçlardan birini döndürür:

`COMPLETE`: Configuration gerekli ve yeterlidir.

`MISSING_CONFIGURATION`: Configuration gerekli ama eksik veya ilgisizdir.

`INCOMPLETE_CONFIGURATION`: Configuration ilgili ama yetersizdir.

`NO_CONFIGURATION_REQUIRED`: Description için Configuration gerekmez.

`UNCERTAIN`: Net karar verilememiştir.

## Kullanılan Teknolojiler

* Java 21
* Spring Boot
* Maven
* Ollama
* qwen3:4b-instruct
* Docker
* Docker Compose
* JUnit / Mockito

## Gereksinimler

Projeyi çalıştırmak için aşağıdaki araçlar gereklidir:

* Java 21
* Docker Desktop
* Ollama
* qwen3:4b-instruct modeli

Modeli indirmek için:

```bash
ollama pull qwen3:4b-instruct
```

Ollama'nın çalıştığını kontrol etmek için:

```bash
curl http://localhost:11434
```

Beklenen cevap:

```text
Ollama is running
```

## Uygulamayı Çalıştırma

Önce proje build edilir:

```bash
./mvnw clean package
```

Windows için:

```powershell
.\mvnw.cmd clean package
```

Docker image oluşturulur:

```bash
docker build -t release-note-api:1.0 .
```

Docker Compose ile uygulama çalıştırılır:

```bash
docker compose up
```

Arka planda çalıştırmak için:

```bash
docker compose up -d
```

## API Kullanımı

Endpoint:
```http
POST http://localhost:8080/api/release-notes/analyze
```
Header:
```http
Content-Type: application/json
```
Örnek request body:
```json
{
  "releaseNote": "*-----Release Comment -----*\n\n*Description:* Product tablosuna stock_status kolonu eklenmiştir.\n\n*Configuration:* DB deki Product tablosuna stock_status kolonunu eklemek için aşağıdaki SQL script kullanılmalıdır:\n\n{code:sql}\nALTER TABLE Product ADD stock_status VARCHAR(20);\n{code}\n\n*Components:* ORDER\n\n*Test environment and status:* Lokal test ortamında test edilmiştir.\n\n*Warning Notes:* Deployment öncesi database script çalıştırılmalıdır."
}
```
## Notlar

Bu proje teknik doğrulama yapmaz. Örneğin SQL script’in gerçekten çalışıp çalışmadığını veya property değerinin sistemde var olup olmadığını kontrol etmez. Projenin amacı, release note içeriğinin Description ve Configuration bölümleri açısından tutarlı ve yeterli olup olmadığını analiz etmektir.
