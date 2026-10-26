# Pet Sell data/preflight boundaries

## Primary evidence (2026-10-26)

The [official API documentation](https://api.hypixel.net/) describes item/inventory data as base64-encoded gzipped NBT. For development only, two bounded reads of [anonymous public auction page0](https://api.hypixel.net/v2/skyblock/auctions?page=0) were decoded with pinned26.2 `NbtIo` and a1MiB/depth32 quota. No API key, account/profile lookup, private inventory, auction action or author service was used. Raw responses were deleted immediately after projection; retained evidence has field names/types and protection facts only, with no player, auction or pet UUID values. This is not a third runtime endpoint.

Current samples included raw `PET` items with `ExtraAttributes` keys `id`, `uuid`, `petInfo` and sometimes `timestamp`. The native item's UUID was present; embedded `petInfo.uuid` was sometimes absent and, where present in these samples, agreed with the item identity. The declared parser therefore binds to the raw item UUID and refuses a conflicting embedded identity rather than inventing an ID from the name or requiring an optional embedded field.

Observed metadata included `type`, `tier`, `exp`, `active`, `candyUsed`, `petSoulbound`, `noMove`, `hideInfo`, `hideRightClick`, and optional attachment/extra/identity fields. In particular, nested `petSoulbound` is not interchangeable with the generic top-level item protection flags. The projection recognizes the observed SPECIAL tier but conservatively protects it. Unknown new schema remains a refusal, never a default unprotected pet.

## Implemented slice

- Strict bounded duplicate-free JSON and primitive types; required flags cannot disappear or become strings. Finite bounded exact experience, non-nil canonical native UUID, optional embedded agreement. Unknown root metadata/attributes and malformed identities are refused.
- Explicit exact type+rarity/experience/count authorization; defaults allow nothing. Active/soulbound/noMove, hidden-info/right-click flags, candies, held items/skins and unreviewed nonempty extras/unique metadata are conservative protection vetoes, not editable name-based estimates.
- Native item/head/raw-ID/UUID/component projection is immutable and does not resolve a profile, contact a service or mutate data. Protected hotbar/equipment/offhand/body/saddle cannot be selected. A complete43-slot baseline, every raw PET projection and unique identity are required; an omitted/duplicate pet cannot be silently ignored.
- Plans privately freeze eligibility and complete inventory data. The native component fingerprint includes the original pet JSON, so subsequent metadata changes invalidate the snapshot. Eligibility data does not prove an executable menu action or a payout.

## Still required

No automatic Pet Sell driver, `/call` sender, transfer/cursor handling, George menu layout/price/confirmation parser, receipt/payout/recovery protocol, settings/service/UI or live inventory/menu test is introduced. A future caller must freshly revalidate native/server identity, all affected inventory/cursor/menu states, actual quantities/prices and explicit disposal policy under the exact leased owner, with bounded single-attempt behavior and safe cancellation/recovery. Current public item data and synthetic native fixtures are **not** live George compatibility or successful-sale evidence. The unfinished feature remains unavailable, and stable2.0.17/settings are untouched.
