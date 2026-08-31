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

### 6) A stateless one-shot chat call
- `ExtensionLlm.getCommunicationService(commsKey, outputTabName)` caches one
  `LlmCommunicationService` per comms key, and that service keeps a `MessageWindowChatMemory`
  (last 10 messages) across calls — a good fit for the add-on's own interactive chat UI, but a
  footgun for callers whose "analysis" is really a series of independent, already
  self-contained prompts (each with its own embedded context, as ours is). Every call after
  the first silently resends prior calls' full prompts as "history", inflating token cost and
  letting one page's traffic content bleed into an unrelated page's analysis.
- We work around this today by calling `removeCommunicationService(commsKey)` after every
  `chat()` call, which drops the cached service (so the next call gets fresh memory) without
  touching the visible chat tab. A first-class stateless variant — e.g. a `chatOnce(String)` on
  `LlmCommunicationService`, or a `getCommunicationService(...)` option that opts out of
  persisted memory — would let add-ons doing one-shot analysis avoid needing to know about this
  cache-eviction workaround at all.
