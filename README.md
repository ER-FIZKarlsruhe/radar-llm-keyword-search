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
