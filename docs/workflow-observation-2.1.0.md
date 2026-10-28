# Workflow observation and due-Pest handoff checkpoint

2026-10-28; development branch only. This is not completion of the global lifecycle matrix, Bazaar/Pet/Pest integrations or live acceptance.

## Book / Supercraft read failures

- Service screen gates distinguish known closed, known open and unavailable. An unavailable read during active work cancels the original task even if its expected menu had previously been recognized. It does not permit another transfer/craft, cursor recovery or menu close. Automatic dispatch stays suspended after an active cancellation.
- A failed screen read with only a queued UI request discards that nonpersistent intent; it does not rewrite saved automatic consent. A subsequently healthy independently enabled automatic timer may still start fresh work. This is not replay of the discarded manual request.
- With no task/request, an unavailable gate has no native side effects or preference writes; a later healthy observation can dispatch normally. A known-open owned recipe/anvil remains supported; failure is not collapsed into that normal state.
- Book lazy-port construction throwing or returning null refuses before a task/root is acquired. An explicit manual request receives the existing not-started notice; no command or success is announced. This narrowly catches factory construction, not task/session/safety-stop callbacks.
- Supercraft ownership queries fail closed on an unreadable menu. Its request catches only preflight observations before acquisition, then lets failed `session.begin` safety-stop errors escape. Failed catalog setup disposes the original task and stops its hold. Session-stop errors remain visible through request, tick and cancel, with state/handle already terminal and no duplicate cleanup.

`BookServiceObservationTest` and `supercraft.SupercraftObservationTest` run actual production services/tasks/session against deterministic faulting ports: idle, pending, owned-menu, null/throwing factory, context/frame/inventory/catalog faults, healthy recovery, known-owned menu progress, failed pause/catalog setup and stop-callback exception identity/once-only cleanup. Existing task/service/manual-intent and dispatch tests remain required. These ports do not establish current native menu formats or server outcomes.

## Pest busy-root dispatch

An idle Pest service now returns before reading UI/native context, temporary tools, pose, targets or trap facts when another gameplay root is held. An explicit manual request is refused at the same boundary, without queuing future work. Busy automatic ticks reset only their poll throttle, not saved consent, cooldown or original Farming state. After a successful owner release, a due Pest can revalidate and start at the same monotonic timestamp; the prior busy poll cannot impose a spurious one-second wait.

`pest.PestBusyRootTest` composes the real service/task/session with a borrowed Supercraft lease and an existing deterministic Pest target. It checks zero busy reads, no manual acceptance, fresh UI/Farming/threshold refusal, unchanged preferences and a new Pest lease bound only to the original manual Farming generation. It is **not** proof that an actual Supercraft job, flight or cross-feature server chain succeeded. Manual/ordinary/world stops and unconfirmed completion never grant a successful handoff.

## Jerry global stop with callback failures

`AutomationClientBridge.suspendForJerry` now invokes the same nine services in the same order through `AutomationStops.run`. A runtime exception from one suspension cannot skip later services' pending-intent cancellation/runtime suspension. After every listed callback has been attempted once, the exact first runtime exception is rethrown with distinct later failures suppressed; throwing the same first instance again cannot trigger self-suppression. The helper does not acquire/release a root, replay actions, retry failed callbacks or catch arbitrary JVM `Error` values.

`test-jerry-stop-faults.py` compiles the actual Bridge method body with controlled service actors and a real AutomationSession/Farming hold. It covers healthy order, a throw at each of the nine positions, all callbacks throwing, and a real failing original-session stop from each of the eight gameplay feature owners. The earlier body fails17 of19 scenarios because later callbacks are skipped; the final body completes all19. `AutomationStopsTest` also checks exact error identity, later suppressed errors, repeated first-error identity, ordered once-only invocation, no-op empty sequence and intentionally unhandled `Error`.

These actors establish dispatcher failure handling, not all services' native cleanup behavior. A service's own earlier failure may still require manual attention; the original error is deliberately not hidden. Other manual/world/shutdown stop entry points have not been converted by this checkpoint, and future executable Pet Sell must join the explicit matrix before release.

## Remaining gates

No new buying, selling, bait, spray, exchange, event feed, settings toggle or current layout is introduced. Other service read/callback faults, remaining stop entry points, delayed packets, native multi-job chains, current Bazaar/George evidence and live verification remain separate open gates. Stable2.0.17 and user settings are unchanged.
