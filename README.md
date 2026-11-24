# Prerequisites
    - Running Ollama
    - System-Prompt-File (see /docs)
    - Configure Model:
        -  ollama create keyword-extractor -f Keyword-Extractor-Model<x>.md
# Structure
## Controllers
- KeywordController
  - /chat
    - Call Ollama model from radar.ollama.models with Text (required per request)
    - Ollama

