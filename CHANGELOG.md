# Changelog
All notable changes to this add-on will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
### Added
- Unit tests for the JSON role-config migration parser (`AnalystOptionsTest`) and for
  `LlmAddonClient`'s reflection-based dispatch (`LlmAddonClientTest`).

### Changed
- Declare the dependency on the official LLM add-on via the `zapAddOn` manifest `dependencies`
  DSL instead of a manual post-build XML patch of `ZapAddOn.xml`.
- Replace the hand-rolled recursive-descent JSON parser used for legacy role-config migration
  with `net.sf.json-lib`, which ZAP core already bundles at runtime.
- Deduplicate the near-identical analysis pipelines in `analyzeRequest`/`analyzeRequestCustom`
  (LLM-configured check, live request, prompt truncation, chat call, result/error reporting)
  into a single shared `submitAnalysis` helper; each method now only builds its own prompt body.
- Status messages now show the LLM add-on's actual configured model name instead of the
  generic literal "LLM".
- Bump `commonlib` dependency to 1.40.0.

### Fixed
- `LlmAddonClient.chat()` unwraps `InvocationTargetException` from the reflective call into the
  LLM add-on's `chat()` method, so a real failure (bad API key, network error, rate limit) shows
  its actual message on the Analyst panel instead of "Error: null".
- `AnalystPanel.updateAnalysis` now also treats the "Sending live request..." status as
  transient, alongside the "Thinking..." statuses; previously it was appended to the permanent
  markdown history (and thus to saved reports / "Save as Alert" prefills) as a bogus finding on
  every single analysis.
- Evict the LLM add-on's cached communication service after each analysis so its rolling
  chat-memory window doesn't silently resend prior analyses' full request/response bodies
  (and their token cost) as "history" on every subsequent call, and doesn't leak one page's
  traffic content into another page's analysis.
- `LlmAddonClient.getExtensionLlm()` dereferenced `Control.getSingleton()` without a null
  check; since that's a plain static field that stays `null` until ZAP explicitly initializes
  it, this could throw instead of reporting "not configured".
- `./gradlew test` was silently running zero tests (no `useJUnitPlatform()`), so the existing
  JUnit 5 test classes were never actually executed. Now wired up correctly.

## [1.2.0] - 2026-02-01
### Added
- Integration with the official ZAP LLM add-on.
- Roles/personas configuration in the AI Traffic Analyst options.

### Changed
- Minimum supported ZAP version is now 2.15.0.
- Analysis requests are sent through the LLM add-on communication service.

### Removed
- Legacy Ollama integration (URL/model selection, HTTP client, and dependencies).
