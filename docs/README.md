ollama create keyword-extractor -f Keyword-Extractor-Model.md


curl -s https://llm.radar-service.eu/ollama/api/chat   -H 'Content-Type: application/json'   -d '{
"model": "keyword-extractor",
"think": false,
"stream": false,
"format": {
"type": "object",
"properties": {
"language": {"type": "string"},
"keywords": {"type": "array", "items": {"type": "string"}}
},
"required": ["language", "keywords"]
},
"messages": [
{
"role": "user",
"content": "Expert consensus statements for the management of COVID-19-related acute respiratory failure using a Delphi method.  BACKGROUND Coronavirus disease 2019 (COVID-19) pandemic has caused unprecedented pressure on healthcare system globally. Lack of high-quality evidence on the respiratory management of COVID-19-related acute respiratory failure (C-ARF) has resulted in wide variation in clinical practice. METHODS Using a Delphi process, an international panel of 39 experts developed clinical practice statements on the respiratory management of C-ARF in areas where evidence is absent or limited. Agreement was defined as achieved when > 70% experts voted for a given option on the Likert scale statement or > 80% voted for a particular option in multiple-choice questions. Stability was assessed between the two concluding rounds for each statement, using the non-parametric Chi-square (χ2) test (p < 0·05 was considered as unstable). RESULTS Agreement was achieved for 27 (73%) management strategies which were then used to develop expert clinical practice statements. Experts agreed that COVID-19-related acute respiratory distress syndrome (ARDS) is clinically similar to other forms of ARDS. The Delphi process yielded strong suggestions for use of systemic corticosteroids for critical COVID-19; awake self-proning to improve oxygenation and high flow nasal oxygen to potentially reduce tracheal intubation; non-invasive ventilation for patients with mixed hypoxemic-hypercapnic respiratory failure; tracheal intubation for poor mentation, hemodynamic instability or severe hypoxemia; closed suction systems; lung protective ventilation; prone ventilation (for 16-24 h per day) to improve oxygenation; neuromuscular blocking agents for patient-ventilator dyssynchrony; avoiding delay in extubation for the risk of reintubation; and similar timing of tracheostomy as in non-COVID-19 patients. There was no agreement on positive end expiratory pressure titration or the choice of personal protective equipment. CONCLUSION Using a Delphi method, an agreement among experts was reached for 27 statements from which 20 expert clinical practice statements were derived on the respiratory management of C-ARF, addressing important decisions for patient management in areas where evidence is either absent or limited."
}
]
}' | jq -r '.message.content'
