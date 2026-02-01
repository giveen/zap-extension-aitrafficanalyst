# Upstream LLM add-on notes (for future PRs/issues)

This add-on depends on the ZAP **LLM** add-on for provider configuration and message exchange.

## Potential enhancements that would benefit advanced add-ons

### 1) Structured prompt support (system/user messages)
- Ability to send multi-part messages (system + user + tool results) instead of a single concatenated string.
- Would let add-ons preserve a stable “system guard” while attaching request/response content as separate messages.

### 2) Response format / output control
- A provider-level option to request a response format (e.g., Markdown/text vs JSON) or to specify a schema.
- Add-ons often want predictable output for UI rendering and follow-on parsing.

### 3) Streaming
- Optional streaming callbacks for progressively updating UI.
- Even if some providers don’t support streaming, add-ons could still benefit where available.

### 4) Better error surface
- A stable, structured error object (provider unreachable, auth failure, quota, invalid config).
- Allows add-ons to show actionable guidance instead of generic exception text.

### 5) Non-interactive / headless usage
- Ability for add-ons to query whether a provider is configured and retrieve a human-readable “what’s missing” message.
- Some of this exists today; having a stable interface/contract for it would help integrations.
