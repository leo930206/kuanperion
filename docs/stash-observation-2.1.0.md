# Stash observation and request-generation checkpoint

2026-10-28, development only. Real Stash service/task/coordinator tests, not a live `/pickupstash` success or complete2.1.0 acceptance claim.

## Failure handling

- Null/throwing context reads before a manual request or an independently received ADDED signal refuse without creating work, sending a command, writing settings or permanently suspending an otherwise idle service. A healthy tick alone cannot replay the failed event; a fresh authorized event/manual request is needed.
- During active work, an unavailable read before a tick or EMPTY/BLOCKED processing cancels that original request, releases its actual command lease and suspends automatic pickup for the current runtime. It does not interpret an unreadable completion message as success or continue holding the coordinator until timeout. Saved automatic consent is preserved; an explicit manual request remains available and explicit re-enable acknowledges suspension.
- The context catch surrounds only the read. Cancellation state/lease revocation precedes feedback, and an exception from cancellation feedback remains visible rather than being mistaken for a context failure.
- ADDED while already active neither resets counters nor consults context. Untrusted/non-signal messages keep their original early refusal. Automatic consent is rechecked after a context read, so a reentrant suspension cannot be bypassed using an earlier enabled observation. This service remains client-thread confined; no cross-thread synchronization guarantee is introduced.

## Original request only

Each accepted StashTask request increments an internal, nonpersistent run ID. The service snapshots this before reads and ticks; observations, completion signals and sender returns from a previous run cannot dispatch, cancel, count attempts for or announce success/start for a newer explicitly requested run. The task additionally binds a sender return to the same original lease and active run. A cancelled/replaced run's accepted/rejected/throwing send result grants no authority over its replacement.

The first send of each independent request resets `everSent`/`lastSent`; the previous run's delay cannot suppress a new request's first command. The configured delay/attempt/timeout bounds within that request remain unchanged. Nested own-send callbacks restore the previous trusted scope, not an unconditional false; after the outermost call returns, ordinary manual-command priority is restored. These mechanisms do not enqueue work, retry an uncertain send, acquire a Farming hold or enable Farming.

## Evidence and limits

- Eleven named pre-implementation model invocations genuinely failed on idle/active reads, old-read/old-signal reentrancy, accepted/rejected/throwing old sender results, inherited delay, feedback error identity, suspension during a read and redundant ADDED observations. A separate nested-sender behavioral RED also preceded its change.
- `StashObservationTest` runs the actual service/task/coordinator with temporary external preference files and deterministic context/sender callbacks. Both null/throwing reads, all tick/EMPTY/BLOCKED routes before and after the first acquired lease, explicit manual/re-enable, original-run isolation, sender outcomes/nesting and fresh timers are covered. Lease release is verified by acquiring a conflicting Pest lease within the original timeout and same world, not only by checking a UI boolean. Old read/send outcomes also cannot invalidate a newly acquired foreign Pest owner. Saved bytes are checked where preservation is part of this failure contract.
- Existing AutomationRuntimeTest, AutomationSafetyTest and StashSuspendTest pass alongside the new models. No native sender, incoming packet schema, UI description, endpoint, account lookup or settings schema is changed by this checkpoint.

Real server pickup results, full multi-feature handoff, remaining native lifecycle tests and the rest of the major-upgrade scope remain open. Installed stable JAR and actual game preferences are not modified by development tests/synchronization.
