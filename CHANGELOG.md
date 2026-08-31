# Changelog
All notable changes to this add-on will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
### Fixed
- Evict the LLM add-on's cached communication service after each analysis so its rolling
  chat-memory window doesn't silently resend prior analyses' full request/response bodies
  (and their token cost) as "history" on every subsequent call, and doesn't leak one page's
  traffic content into another page's analysis.

### Changed
- Declare the dependency on the official LLM add-on via the `zapAddOn` manifest `dependencies`
  DSL instead of a manual post-build XML patch of `ZapAddOn.xml`.

## [1.2.0] - 2026-02-01
### Added
- Integration with the official ZAP LLM add-on.
- Roles/personas configuration in the AI Traffic Analyst options.

### Changed
- Minimum supported ZAP version is now 2.15.0.
- Analysis requests are sent through the LLM add-on communication service.

### Removed
- Legacy Ollama integration (URL/model selection, HTTP client, and dependencies).
