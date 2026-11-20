Wenn ich die Schlagworte als JSON zurückbekommen habe, wie könnte ich weitermachen?
Ich hätte gerne einen http-Endpunkt dem ich den Text übergebe. Es werden zuerst über ollama die schlagworte extrahiert und mit diesen Schlagworten dann in tib und/oder gnd nach Normdaten gesucht.
Schlag eine Programmiersprache vor
12s nachgedacht

Ich würde dafür Java 21 mit Spring Boot verwenden.

Da du bereits viel mit Java, REST-APIs, Jackson und Spring arbeitest, passt das gut in deine bestehende Umgebung. Für einen ersten Prototyp wäre Python mit FastAPI zwar etwas schneller umzusetzen, für einen dauerhaft betriebenen Service würde ich in deinem Fall Java bevorzugen.
1. Vorgeschlagene Architektur

HTTP-Client

POST /api/keywords/enrich mit Ausgangstext

Spring Boot – Keyword Controller

Validierung, Orchestrierung, JSON-Antwort

Ollama Service

Dein Modell keyword-extractor über /api/chat

Schlagwortkandidaten

JSON mit term, type und language

GND Service

lobid-gnd API

TIB Service

Terminology API

Matching Service

Begriffsgleichheit, Synonyme, Kontext, Trefferbewertung

JSON-Ergebnis

Schlagwörter mit GND-IDs, TIB-URIs und möglichen Alternativtreffern
2. Die benötigten APIs

Du kannst alle drei Systeme direkt über HTTP ansprechen.

System


Schnittstelle


Zweck

Ollama


POST /api/chat


Schlagwörter extrahieren

GND


GET /gnd/search


Normdatensätze suchen

TIB


GET /api/search


Ontologiebegriffe suchen
Ollama

Deinen vorhandenen Endpunkt kannst du direkt weiterverwenden:
https://llm.radar-service.eu/ollama/api/chat

Du sendest den Ausgangstext und erhältst das Schlagwort-JSON aus message.content. Für einen zuverlässigen Parser würde ich ein JSON-Schema im format-Parameter verwenden.
GitHub
GND über lobid

Beispiel:

curl -sG 'https://lobid.org/gnd/search' \
--data-urlencode 'q=Katalyse' \
--data-urlencode 'format=json'

Du bekommst JSON-LD zurück mit Normdatensätzen, unter anderem Identifikatoren und bevorzugten Benennungen. Die API unterstützt außerdem Feldsuchen und Filter.
GitHub

Für eine spätere automatische Zuordnung ist auch die lobid-Reconciliation-API interessant:

https://lobid.org/gnd/reconcile
TIB Terminology Service

Beispiel:

curl -sG 'https://api.terminology.tib.eu/api/search' \
--data-urlencode 'q=Katalyse'

Die TIB bietet eine REST-Suche über Ontologiebegriffe. Insbesondere kannst du unterschiedliche Vokabulare durchsuchen und zu den gefundenen Begriffen ihre IRIs ermitteln.
GitHub+1

Für deine Schwerpunkte sind auch die TIB-Sammlungen NFDI4Chem und NFDI4Culture interessant.
terminology.tib.eu
3. Wie sollte das Matching funktionieren?

Das ist vermutlich die anspruchsvollste Komponente.

Ich würde ein mehrstufiges Verfahren implementieren:

Priorität


Verfahren


Beispiel

1


Exakter Treffer


„Katalyse“ = „Katalyse“

2


Alternative Benennung


„Reibeisenhaut“ → „Keratosis pilaris“

3


Normalisierte Schreibweise


Groß-/Kleinschreibung, Bindestriche, Pluralformen

4


Semantische Ähnlichkeit


Begriffskandidat passt zur Definition eines Normdatensatzes

5


Kontextprüfung


Ist der gefundene Begriff tatsächlich im Ausgangstext gemeint?

Besonders wichtig: Ein Treffer bei der GND oder TIB ist nicht automatisch ein korrekter Treffer.

Beispielsweise kann der Begriff „Automat“ je nach Kontext technische Geräte, mechanische Figuren oder andere Konzepte bezeichnen.

Deshalb würde ich nicht einfach den ersten Suchtreffer übernehmen, sondern mehrere Treffer sammeln und bewerten.

Für den Anfang reicht eine regelbasierte Bewertung. Später könntest du zusätzlich Embeddings oder einen Reranker einsetzen.
4. So könnte dein HTTP-Endpunkt aussehen

Request:
POST /api/keywords/enrich
Content-Type: application/json

{
"text": "Untersuchung der katalytischen Eigenschaften von Zeolithen bei der Methanolumwandlung.",
"sources": ["gnd", "tib"],
"maxKeywords": 10
}

Response (illustrativ, keine echten Normdaten-Treffer):

{
"language": "de",
"keywords": [
{
"term": "Zeolith",
"type": "erschlossen",
"matches": [
{
"source": "gnd",
"label": "Zeolith",
"id": null,
"uri": null,
"matchType": "exact",
"score": 1.0
}
]
},
{
"term": "Methanolumwandlung",
"type": "direkt",
"matches": []
}
]
}

Dabei ist score ein von deinem Matching-Service berechneter Wert und keine von GND oder TIB gelieferte Wahrscheinlichkeit.

In der tatsächlichen Ausgabe sollten id und uri nur mit Werten aus den gefundenen Normdatensätzen befüllt werden. Ein fehlender Treffer wird nicht künstlich ergänzt.
5. Java-Projektstruktur

Ich würde das Projekt relativ schlank halten:
keyword-enrichment/
├── src/main/java/de/fiz/keywords/
│   ├── KeywordApplication.java
│   ├── controller/
│   │   └── KeywordController.java
│   ├── service/
│   │   ├── OllamaService.java
│   │   ├── GndService.java
│   │   ├── TibService.java
│   │   ├── MatchingService.java
│   │   └── KeywordEnrichmentService.java
│   └── model/
│       ├── Keyword.java
│       ├── NormdataMatch.java
│       └── EnrichmentResponse.java
└── src/main/resources/
└── application.yml

Technisch würde ich einsetzen:

    Java 21

    Spring Boot 3.x oder eine zu deiner Laufzeit passende neuere Version

    Spring WebClient für die externen HTTP-Aufrufe

    Jackson für JSON-Verarbeitung

    Gradle als Build-System

Die GND- und TIB-Anfragen kannst du parallel ausführen. Bei zehn Schlagwörtern und zwei Normdatenquellen entstehen bis zu 20 Suchanfragen. Ich würde die Parallelität begrenzen, Timeouts setzen und identische Suchanfragen cachen.

Für den Betrieb würde ich zusätzlich eine klare Fehlerbehandlung vorsehen: Ist beispielsweise der TIB-Service nicht erreichbar, kann die Antwort trotzdem GND-Treffer enthalten, sollte aber einen entsprechenden Hinweis zurückgeben.
6. Eine wichtige Architekturentscheidung

Ich würde den Endpunkt so gestalten, dass sich zwei unterschiedliche Arbeitsweisen unterstützen lassen:

Variante A: Vollständige Verarbeitung

POST /api/keywords/enrich

Text → Ollama → GND/TIB → Matching → JSON

Variante B: Nur Normdaten-Matching

POST /api/keywords/match

Vorhandenes Schlagwort-JSON → GND/TIB → Matching → JSON

Das wäre für deine Entwicklung sehr hilfreich. Du kannst die Qualität der Normdaten-Recherche unabhängig vom LLM testen und später auch Schlagwörter aus anderen Quellen verarbeiten.

Meine Empfehlung: Starte mit Java 21, Spring Boot und diesen beiden Endpunkten. Implementiere zunächst die GND- und TIB-Suche mit exaktem und synonymbasiertem Matching. Erst danach würde ich ein LLM-basiertes Reranking ergänzen. So bleiben die Entscheidungen nachvollziehbar und du kannst für jeden Begriff prüfen, warum eine bestimmte GND-ID oder TIB-URI zugeordnet wurde.