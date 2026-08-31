## AI Traffic Analyst for OWASP ZAP

A security analysis add-on for OWASP ZAP that augments ZAP by feeding live request/response pairs into the official **ZAP LLM add-on** to produce focused, OWASP-aware analysis.

---

## 🤖 Features

- **Live Analysis:** Clones and resends the selected request to capture a fresh response before analysis.
- **OWASP-first:** Prompts and reporting are rooted in the OWASP Top 10 (2021–2026) framework for focused vulnerability discovery.
- **Provider-agnostic:** Delegates all LLM communication to the official **ZAP LLM add-on**, so it works with any provider configured there (local or remote) — this add-on has no provider config of its own.
- **Model-aware status:** Status messages show the actual configured model name (e.g. "Querying claude-sonnet-5...") rather than a generic placeholder.
- **Rich UI:** Persistent, Markdown-rendered analysis tab with configurable appearance and high-contrast iconography.
- **Configurable roles:** Customize analyst roles/prompts from the Options panel—no recompilation required.
- **Session memory:** Keeps a rolling summary of the last few findings in the session to give follow-up analyses context.

---

## 🛠 Installation & Setup

### Prerequisites

- OWASP ZAP **2.15.0+**.
- The official **LLM** add-on (`org.zaproxy.addon.llm`) installed and enabled — this add-on declares a hard manifest dependency on it and won't load without it.
- An LLM provider configured via **Tools → Options → LLM**.

Install the LLM add-on from **Manage Add-ons → Marketplace → LLM** first. If it isn't listed in your Marketplace yet, build it from source:

```bash
git clone https://github.com/zaproxy/zap-extensions.git
cd zap-extensions
./gradlew :addOns:llm:jarZapAddOn
```

Then install the generated `.zap` via **Manage Add-ons → Install Add-on from File…**, and restart ZAP.

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

1. Configure the LLM provider: **Tools → Options → LLM**.
2. (Optional) Configure prompts/roles: **Tools → Options → AI Traffic Analyst**.
3. Analyze: Right-click any request in ZAP History and choose **AI Analyst → Analyze GET / Analyze POST / Custom Analysis...**. The add-on clones and resends the request to collect a live response, then sends the combined request+response to the configured provider. **Custom Analysis** additionally lets you supply your own focus instructions and choose whether to include the request, the response, or both.
4. Review: Results appear in the **AI Analysis** tab, rendered as Markdown.

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

## Release Smoke Test Checklist

Use this checklist to quickly validate a packaged `.zap` before publishing.

- Start OWASP ZAP **2.15.0+**.
- Install the **LLM** add-on (Marketplace if available; otherwise build/install it from `zap-extensions` as described above), then restart ZAP.
- Configure a provider in **Tools → Options → LLM**.
- Install this add-on: Manage Add-ons → Install Add-on from File… → select the built `.zap` in `build/zapAddOn/bin/`.
- Generate some traffic (browse a site or use a sample request) so History has entries.
- Right-click a History entry → **AI Analyst** → run **Analyze GET/POST** (and optionally **Custom Analysis...**).
- Confirm output appears in the **AI Analysis** tab, renders as Markdown, and the status line names the actual configured model.

Negative checks:
- Temporarily remove/disable the provider config in **Tools → Options → LLM**, then re-run analysis and confirm the add-on shows a clear "LLM not configured" guidance message.
- Disable the LLM add-on entirely and confirm this add-on either fails to load (per its manifest dependency) or reports the LLM add-on as missing rather than throwing.

## Notes & Troubleshooting

- If analysis says the LLM is not configured, configure it via **Tools → Options → LLM**.
- If you want private/local-only usage, configure a local provider in the LLM add-on (for example, a locally hosted model provider).
- If this add-on won't load, check that the LLM add-on is installed and enabled first — it's a hard manifest dependency, so ZAP won't load this add-on without it. Also check the ZAP log for errors.
- Each analysis is sent as a fresh, memory-free call to the LLM add-on's chat service — findings from one request are not carried into another's conversation history (only the short on-panel "session context" summary is reused, and only within this add-on).

---

## Attribution

Vibe-coded and iterated with a human-in-the-loop development process. For questions or to contribute, open an issue or submit a pull request.