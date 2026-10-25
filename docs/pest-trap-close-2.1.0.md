# Delayed server trap-menu close — 2.1.0 development

## Regression

After a release click, the server can leave its menu visible for another tick. AutomationSession then claims the container again; a later vanilla server close previously made session.poll cancel the original Farming hold before the Pest task could process release proof. A new regression executes this precise poll/task ordering and failed on the old implementation (DELAYED_SERVER_CLOSE_LOST_ROOT).

## Narrow correction

Only a Pest trap currently waiting for a confirmed release can issue an opaque one-use close ticket. Native packet HEAD must see the same world, exact owned menu/ID, current Pest handle and bounded pending-click time. Native TAIL must independently see the same packet ID/world, no screen and the vanilla player inventory container after application. Only then may the task clear its exact expected-menu claim and continue waiting under the original hold.

This does not create a general null-menu exemption, new click/command permission, automatic GUI close, cursor repair or Farming rearm. Manual/local disappearance, ignored/mismatched IDs, revoked roots, focus/world changes and stale/replayed tickets cannot confirm a server close. Off-thread callbacks do not mutate the observation cache. Each matching TAIL consumes its ticket even if validation fails. Independent fresh release/count-or-new-pest evidence and physical empty cursor are still required; a close packet alone is not release success.

Tests cover delayed close with real AutomationSession polling, manual/local close, wrong/ignored IDs, rejected-TAIL replay, root invalidation and original trap/release regressions. Native26.2 getContainerId and applied HEAD/TAIL Mixin probes are checked separately. No game-server release or live format validation is claimed. Future Visitor/Supercraft close handling must use its own explicitly scoped workflow proof, not borrow Pest's ticket.
