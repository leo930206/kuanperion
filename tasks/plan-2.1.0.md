# 2.1.0 implementation plan

Source of truth: docs/spec-2.1.0.md, approved user scope. Existing tasks/plan.md describes historical 2.0 work; its old “no gameplay changes” restriction is not the scope of this approved 2.1 migration.

1. Safety foundation: source/dependency allowlist, task ownership and context guards, atomic preferences, menu identity contracts, existing Farming handoff/resume token and isolation tests. First small vertical slice: Stash signal -> guarded pickup -> bounded retry -> terminal reply, no movement/GUI effects.
2. Inventory/commerce infrastructure and Trade: snapshots/item identity, protected allowlists, spend reservation/receipt accounting, Bazaar acquisition, then Auto Sell, Book Combine, Pet Sell, Junk. Each independently off by default and genuinely wired before presenting it as available.
3. Pest core: tab/chat observation, trigger/threshold, vacuum/tool and entity targeting, flight/pathing/rotation using only server-granted abilities, bounded cleanup and return. Integrate notification-independent UI as Pest, retain old notification state.
4. Pest extensions: traps, bait, exchange, spray and crop/event policy, one owner and due-task handoffs, no additional unrelated systems.
5. Greenhouse and Composter: each complete menu/movement workflow, source/material checks, intervals, cancellation and return, Farming page modules.
6. Visitor and Supercraft: valuation/acquisition/submission and recipe/quantity workflows respectively. Visitor's own notifications remain independent; Supercraft in Other.
7. Full integration/release: Farming/Pest/Greenhouse/Visitor/Composter and Mining/Trade/Other navigation, task priorities/recovery/legacy pause interactions, all settings migration, dependency/network/secret audit, offline runtime and clearly identified live-test gaps, release/rollback proof, 2.1.0 publication only after functional completion.

Checkpoint every tested vertical slice, not a giant unverified port. A decompiled class compiling is not proof of correct server behavior. Reuse only audited leaf functionality, replacing upstream global dependencies with local typed adapters. Early incomplete builds stay on feature/2.1.0; do not replace the user's installed version.
