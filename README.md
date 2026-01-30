## AI Traffic Analyst for OWASP ZAP

A high-performance, local-LLM-powered security analysis add-on for OWASP ZAP. This tool augments ZAP by feeding live request/response pairs into local models (for example `llama3`, `pentest-ai`, or `qwen2.5`) to produce focused, OWASP-aware analysis.

---

## 🤖 Features

- **Live Analysis:** Clones and resends the selected request to capture a fresh response before analysis.
- **OWASP-first:** Prompts and reporting are rooted in the OWASP Top 10 (2021–2026) framework for focused vulnerability discovery.
- **Privacy-centric:** Runs locally via Ollama so traffic and findings stay on your machine/network.
- **Hardware-accelerated:** Designed to leverage local accelerators for high throughput (tested on high-end GPUs).
- **Rich UI:** Persistent, Markdown-rendered analysis tab with configurable appearance and high-contrast iconography.
- **Configurable:** Hot-swap models, system prompts, and API endpoints from the Options panel—no recompilation required.

---

## 🛠 Installation & Setup

### Prerequisites

- OWASP ZAP (latest recommended).
- Ollama running locally (default: `http://localhost:11434`).
- Desired models available locally (e.g. `ollama pull llama3:70b`).

### Build from Source

```bash
git clone https://github.com/your-username/aitrafficanalyst.git
cd aitrafficanalyst
./gradlew clean jarZapAddOn
```

The generated `.zap` bundle will be placed in `build/zapAddOn/bin/`.

Common dev commands:

```bash
./gradlew spotlessApply
./gradlew test
./gradlew jarZapAddOn
```

Install the add-on in ZAP via **Manage Add-ons → Install Add-on from File…** and select the `.zap` file in `build/zapAddOn/bin/`.

---

## How to Use

1. Configure: Tools → Options → AI Traffic Analyst. Set the Ollama URL, choose a model and adjust the system prompt.
2. Refresh Models: Click `Refresh Models` in the Options panel to query available models from your Ollama instance.
3. Analyze: Right-click any request in ZAP History and choose **AI Analyst**. The add-on will clone and resend the request to collect a live response, then send the combined request+response to the local model.
4. Review: Results stream into the **AI Analysis** tab. 

---

## VIBE CODED

This project was *vibe-coded into existence*.  
I am not a traditional programmer — and I’m unapologetic about it.

- **Planning Agent:** Gemini 3 Pro 
- **Coding Agent:** GPT-5-mini  

### Human-in-the-Loop Vibe Coding Process

I use a deliberate **Human-in-the-Loop (HITL)** workflow to keep control of design,
security assumptions, and intent while letting agents handle implementation details.

1. **Ideation & Discovery**  
   I start with the Planning Agent using a prompt like:  
   > *“I’m thinking of making an XYZ project. Ask me questions to help plan and design this out.”*  
   This forces clarity before any code is written.

2. **Architecture & Breakdown**  
   I ask the Planning Agent to produce a **reviewable architecture and development plan**, broken down into:
   - Major development phases
   - Sub-tasks per phase  
   I explicitly remind the agent:  
   > *“I’m a security engineer, not a programmer — explain this in terms I can understand.”*

3. **Human Approval Gate**  
   I review, question, and approve the plan **before** any implementation begins.

4. **Execution by Coding Agent**  
   The approved planning prompt is passed to the Coding Agent, with instructions to:
   - Implement only the approved phase or sub-task
   - Return a **completion summary** explaining:
     - What was done
     - Which phase/sub-task was completed

5. **Review & Refinement**  
   Results are sent back to the Planning Agent for:
   - Review
   - Design feedback
   - Security considerations
   - Follow-up questions or suggested changes

6. **Iterative Loop**  
   Steps 4–5 repeat until the project converges.

This loop keeps the system **intent-driven, explainable, and auditable**, while still
moving fast — and without pretending I suddenly became a full-time software engineer.

---

## Notes & Troubleshooting

- If the model list is empty, verify Ollama is reachable at the URL configured in Options.
- For private/local-only usage, ensure your firewall and network settings allow local loopback traffic between ZAP and Ollama.
- If the plugin doesn't load, check `ZapAddOn.xml` version constraints and the ZAP log for errors.

---

## Attribution

Vibe-coded and iterated with a human-in-the-loop development process. For questions or to contribute, open an issue or submit a pull request.