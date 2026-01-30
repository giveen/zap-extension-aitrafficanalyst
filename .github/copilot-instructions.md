<todos title="Security & Quality Review Tasks" rule="Review steps frequently throughout the conversation and DO NOT stop between steps unless they explicitly require it.">
- [x] sanitize-markdown-rendering: Sanitize and escape Markdown→HTML rendering in `AnalystPanel` using CommonMark.escapeHtml(true) and Jsoup clean; add unit test for XSS vectors. 🔴
  _Reused Parser/HtmlRenderer instances and applied Jsoup.safelist.basic() to rendered HTML._
- [x] add-prompt-guard-and-size-limit: Add immutable system guard at start of prompts and enforce a 128KB max prompt size before sending to Ollama. 🔴
  _Prepend SYSTEM guard and truncate/mark prompts exceeding 128KB; show user-visible truncated warning._
- [x] limit-history-memory-growth: Prune/rotate `fullHistoryMarkdown` in `AnalystPanel` to cap growth (2MB) and keep newest entries. 🟡
  _On append, trim head and insert '[... truncated]' marker to keep newest content under MAX_HISTORY_CHARS._
- [x] targeted-exception-handling: Replace broad catch-all exceptions with targeted handlers for network timeouts, UnknownHost, JSON errors, and IO; show concise UI messages. 🔴
  _Catch UnknownHostException, SocketTimeoutException, JsonProcessingException, IOException and map to friendly status updates._
- [x] validate-ollama-url-ssrf-protection: Validate configured Ollama URL to allow only loopback/localhost (127.0.0.1, ::1) or allowlist; refuse save if invalid. 🔴
  _Implemented isAllowedOllamaUrl(String) with URI parsing and host checks; used in `AnalystOptionsPanel` save flow._
- [x] use-executorservice-and-lifecycle: Replace `new Thread()` usage with an ExecutorService in `ExtensionAiAnalyst`; shut down in `unload()`. 🔴
  _Created daemon cached thread pool accessible via `getExecutor()` and shutdown in `unload()`._
- [x] reuse-commonmark-and-htmlrenderer: Reuse CommonMark `Parser` and `HtmlRenderer` instances as fields in `AnalystPanel` to reduce GC pressure. 🟡
  _Made parser/renderer final fields and used escapeHtml(true) for input safety._
- [x] reuse-okhttpclient-or-share: Create shared `OkHttpClient` at extension level and pass to `OllamaClient` to reuse connection pools. 🟡
  _Added shared client in `ExtensionAiAnalyst` with constructor overload in `OllamaClient`._
- [x] i18n-messages-and-strings: Move hardcoded UI strings (buttons, status messages, tooltips) into `Messages.properties` and fix malformed control-character encodings. 🟡
  _Migrated strings to i18n and corrected emoji/unicode escapes in `Messages.properties` to remove stray control characters._
- [x] bufferedimage-defensive-harding-and-tests: Add defensive checks around `ImageIO.read()` in `AnalystPanel` with fallback to `ImageIcon`; add unit test for image-scaling and g2 disposal behavior. 🟢
  _Fallback to ImageIcon when ImageIO.read returns null; added `AnalystPanelImageTest` exercising scaling and disposal._
</todos>

<!-- Auto-generated todo section -->
<!-- Add your custom Copilot instructions below -->
