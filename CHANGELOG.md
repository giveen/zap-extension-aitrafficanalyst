# Changelog
All notable changes to this add-on will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

## [1.2.0] - 2026-02-01
### Added
- Integration with the official ZAP LLM add-on.
- Roles/personas configuration in the AI Traffic Analyst options.

### Changed
- Minimum supported ZAP version is now 2.15.0.
- Analysis requests are sent through the LLM add-on communication service.

### Removed
- Legacy Ollama integration (URL/model selection, HTTP client, and dependencies).
