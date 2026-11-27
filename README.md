`# Prerequisites
    - Running Ollama
    - System-Prompt-File (see /docs)
    - Configure Model:
        -  ollama create keyword-extractor -f Keyword-Extractor-Model<x>.md
# Structure
## Controllers
- KeywordController
  - POST /extract
    - Request:
      - text, terminology(tib, gnd, both), model (see /src/main/resources/application.properties - radar.ollama.models)
    - Response:
      - {
        "Turmbau zu Babel": {
        "iri": "https://d-nb.info/gnd/4143847-4",
        "label": "Turmbau zu Babel",
        "best_term": "Turmbau zu Babel",
        "ontology_name": null,
        "source": "gnd",
        "keyword_scheme": "GND",
        "scheme_uri": "https://d-nb.info/gnd/",
        "classification_code": "4143847-4"
        }, ...

# Configuration
Create new Ollama-Model:
jq -n --rawfile prompt system-prompt.txt '{
model: "keyword-extractor",
from: "qwen3:8b",
system: $prompt,
parameters: {
temperature: 0,
num_ctx: 16384
},
stream: false
}' | curl -X POST \
https://llm.radar-service.eu/ollama/api/create \
-H 'Content-Type: application/json' \
-d @-

# Problems
I complained about not finding Institutions.
Answer:
Die drei Institutionen fehlen, weil der Prompt sie gar nicht vorsieht und an einer Stelle sogar ausschließt. Ich habe nichts geändert.

Gründe im Prompt:
1. Institutionen sind nicht vorgesehen. Aufgezählt werden nur „Gegenstände, Stoffe, Methoden sowie die Personen, Orte, Werke und Epochen“. Körperschaften wie Museen, Hochschulen oder Labore kommen nicht vor, also gibt es sie für das Modell nicht.
2. UCLAB wird ausdrücklich ausgeschlossen: durch „Keine Projektnamen“, „Abkürzungen ausschreiben“ und „Jedes Schlagwort muss für sich allein etwas über das Thema sagen“. Die Fachhochschule Potsdam ist im Text nur Projektinitiator und sagt in diesem Sinn nichts über das Thema.
3. Die 10 Plätze sind voll. Das Modell soll die wichtigsten Begriffe zuerst nennen und füllt alle Plätze mit Themenbegriffen wie Kleidung, Kostüm und Tracht. Selbst eine nachrangig eingestufte Institution bleibt außen vor.

Bibliothekarisch ist das teilweise sogar Absicht. In der Sacherschließung ist eine Körperschaft nur Schlagwort, wenn es um sie geht.
- Das Germanische Nationalmuseum besitzt die Textilsammlung, die untersucht wird. Es wäre als Schlagwort vertretbar.
- UCLAB und FH Potsdam haben den Datensatz erstellt. Sie gehören in die Metadaten zu Beteiligten und Affiliation, nicht in die Schlagwörter.
- Auch das Gold für diesen Datensatz enthält keine Institution: 3D, Kunstgeschichte, Kleiderforschung, Photogrammetrie.

