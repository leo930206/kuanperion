# Explicit trap destination (2.1.0 development)

Trap IDs are not plot IDs. The supplied upstream PestTrapManager uses a separate PEST_TRAPS_PLOT setting (default string zero); its trap identifier lookup is independent. Never turn Full Traps: #7 into a request to plot7.

Pest policy now has trapPlot -1 (unset) through24; zero is Barn. Missing old configuration stays unset even when clearTraps and travelPlots are enabled. Every UI/policy edit preserves the explicit destination. Unknown/malformed/fractional/string/out-of-range values fail closed and are not silently repaired to Barn.

A trip additionally requires clearTraps, travelPlots greater than zero, fresh known nonempty Full Traps, a known original plot, a valid Vacuum, native Garden and server-granted flight. The destination shares the existing visited-destination budget; there are no automatic retries. The same root lease and original Farming token cover typed travel, fresh absolute server position + expected plot + current pose, a bounded five-second marker loading observation, the existing exact loaded-trap release/capture, and confirmed original plot/exact original position/tool/view restoration.

No observed marker after the bounded wait means INCOMPLETE and a guarded return, not an invented cleared Garden or an unrelated plot search. Positive global alive counts may already include trapped pests, so when no loaded Pest exists, a known full local/configured trap is considered rather than stalling forever on an unreachable count. This does not authorize travel from an unknown Full Traps field.

Pure tests cover zero alive/unknown loaded entities, no ID-to-plot inference, old configuration no added authority, no predicted arrival, bounded missing-marker return, manual cancellation, positive-alive local traps without travel, and original plot24 corner -> configured Barn0 trap7 -> owned release -> regular capture -> confirmed plot24 segmented exact return. Offline native/UI and live-server limitations remain separate; bait/menu changes still require current-format validation. Installed2.0.17 is unchanged.
