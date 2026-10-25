# Jerry automatic home compatibility (2.1.0 development)

The new outbound-command observer originally treated the existing automatic Jerry sethome as a manual takeover. With a new task holding Farming, that could stop Farming before the original Jerry stop-toggle handler saw its state: stop disabled could still stop, and stop enabled could skip the pause marker. Stable2.0.17 does not contain this observer and is unchanged.

Only the existing automatic Jerry recording path now calls a typed literal-sethome bridge sender. It scopes and restores the previous own-command/dispatch flags, uses the ordinary native command API and verifies its outgoing event, emits no extra notification and never acquires a lease or changes preferences. No arbitrary command exemption API is added. Manual hotkeys and ordinary commands still use their original path and preempt new work.

The actual old/new private sender, Jerry handler, outbound observer and stop/marker methods run together with real AutomationSession/FarmingHandoff in the regression harness. Pest/Greenhouse/Composter held-Farming cases cover independent stop and chat switches, one exact command/message, preserved marker order and subsequent manual takeover. A negative mutation back to the old manual sender must fail this same regression. Separate actual-method tests cover send errors, unobserved dispatch, client-thread/player guards and nested flag restoration.

This is not the entire release priority matrix: stopping manual or standalone new jobs while Farming is not armed, and idle standalone auto timers after a global Jerry stop, remain tracked separately before release. No live server claim or stable installation change.
