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
./gradlew clean copyZapAddon
```

The generated `.zap` bundle will be placed in `build/zapAddOn/bin/`. Drag-and-drop that file into a running ZAP instance (Help → Check for Updates → Local add-on or the ZAP add-on manager).

---

## How to Use

1. Configure: Tools → Options → AI Traffic Analyst. Set the Ollama URL, choose a model and adjust the system prompt.
2. Refresh Models: Click `Refresh Models` in the Options panel to query available models from your Ollama instance.
3. Analyze: Right-click any request in ZAP History and choose **Analyze**. The add-on will clone and resend the request to collect a live response, then send the combined request+response to the local model.
4. Review: Results stream into the **AI Analysis** tab (Markdown-rendered). Use `Save Report` or `Clear All` in the panel toolbar.

---

## Human-in-the-Loop (HITL) Workflow

This project was developed using a deliberate Human-in-the-Loop (HITL) workflow to preserve security intent and reviewer control:

- **Ideation & Planning:** A planning pass defines the goals, phases, and acceptance criteria before implementation.
- **Architecture & Breakdown:** Work is split into discrete phases and sub-tasks; each phase is reviewed and approved.
- **Human Approval Gate:** No code is implemented until the design is approved.
- **Execution by Coding Agent:** Approved tasks are implemented by the coding agent and returned with a completion summary.
- **Review & Iterate:** Results are reviewed for security/design, and the cycle repeats until the project converges.

> This repository includes a short "vibe" section to acknowledge the workflow used for rapid prototyping. The core implementation remains transparent and reviewable.

---

## Notes & Troubleshooting

- If the model list is empty, verify Ollama is reachable at the URL configured in Options.
- For private/local-only usage, ensure your firewall and network settings allow local loopback traffic between ZAP and Ollama.
- If the plugin doesn't load, check `ZapAddOn.xml` version constraints and the ZAP log for errors.

---

## Attribution

Vibe-coded and iterated with a human-in-the-loop development process. For questions or to contribute, open an issue or submit a pull request.