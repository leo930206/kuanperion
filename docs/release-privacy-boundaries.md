# Development release privacy boundaries

Checked 2026-10-26 against this repository and its local development archive; no server/account/author-service request was needed.

## Current outgoing request code

- Demand-only anonymous `GET https://api.hypixel.net/v2/resources/skyblock/items` for explicit item identity and `GET https://api.hypixel.net/v2/skyblock/bazaar` for prices. Both have one fixed request/send boundary, Accept-only application header, bounded body/deadline/cache and no identity/cookie/proxy configuration. The runtime callers require their own live task authority. Catalog data itself grants no gameplay permission.
- Existing notifications `POST` only to the user's explicitly configured webhook. Responses are discarded except status/error reporting; the response cannot issue gameplay commands. The existing URL setting accepts user-chosen HTTP(S) endpoints (not an enforced Discord-domain allowlist); this audit does not silently change that preference behavior or claim all user endpoints are trustworthy. The secret URL/token is never included in the artifact or request-error logging.
- The XML parser's `http://apache.org/xml/features/disallow-doctype-decl` string names a disabled-DOCTYPE feature; it is not an HTTP request. Curated skin identities are compared with received local profile data, not fetched/resolved.

No Aether runtime/bootstrap, account/IRC/telemetry/remote-control/updater or original client JAR is imported. Required upstream GPL attribution/repository links remain in source comments and distributed license notices; these are not runtime connections and are intentionally not stripped.

## Repeatable gates

`python3 scripts/test-release-audit.py` runs the actual source audit and bounded in-memory negative fixtures. It rejects extra endpoints/clients/raw sockets, authenticated or uploading public lookups, upstream classes, agent manifests, unsafe/duplicate archive paths, known private/runtime paths, test payloads, altered/missing dependencies and missing source license/metadata copies. Candidate fixtures are never executed or loaded as Java classes/native libraries.

`python3 scripts/audit-release.py --jar build/libs/kuanperion-2.1.0.jar` audits actual production Java25 constant pools, package/endpoint/known-networking references, entrypoint/game/version metadata, archive bounds and five byte-pinned LWJGL NanoVG3.4.1 rendering dependencies. The only nested JARs must be exactly those five declared files; the GPL/source notices must equal their source files. The project build runs this gate after creating its archive, failing before any release/install step. The ordinary test script includes the negative/source gates.

## Limits

These are static regression guards plus manual code review, **not** an OS network sandbox, formal information-flow proof, universal secret scanner, dynamic traffic capture, live-server compatibility test or guarantee about arbitrary obfuscated future code or user-provided endpoints. Constant-pool fixtures test parsing/boundaries, not JVM verification; compilation, actual native tests, source/payload equality and isolated UI tests remain separate required gates. A changed dependency, new endpoint or version requires explicit review and an intentional gate update rather than a broad bypass. This audit does not make the unfinished2.1.0 upgrade release-ready.
