# Settings entry / task coexistence checkpoint

The hotkey and local `/kuanperion` command share the actual guarded toggle. When a shared task is active, a non-inventory server container is still open, a carried item exists, or a loading overlay is present, opening settings is refused with local feedback. This is a read-only check: it does not close a menu, move an item, cancel a root, invalidate its Farming hold, send a command or queue an automatic later open. After ending the task and manually closing the container/returning the carried item, the user can request settings again.

Closing an already open MainGUI does not invoke the opening guard. With no new shared task, ordinary legacy Farming and Mining retain their existing non-pausing settings behavior. Title-screen local configuration remains possible. Command-only Stash does not acquire a shared gameplay root or server menu and is not newly blocked by this gate.

`scripts/test-settings-task-coexistence.py` compiles and executes the actual production toggle and read-only native gate with boundary fixtures, including all seven current shared-root owners, full cursor/container identity, overlay/thread, title and legacy context cases. Existing settings attack/pause/UI and typed native action tests remain required. These fixtures do not perform server interactions.

This is **not** the complete coexistence release gate. In-flight vanilla menu packets after an earlier cancelled command are not suppressed or fabricated as acknowledged. Per-feature manual requests made from inside settings, all delayed server responses and genuine server-menu transitions require further audit and live validation. No claim of uninterrupted new GUI workflows, automatic screen restoration or all-module completion follows from this checkpoint.

## Pest manual request follow-up

Pest's manual action now accepts one nonpersistent intent while settings is open, without acquiring a root, holding Farming, reading/replaying a flight target or touching a tool/input/menu. The existing native screen-null predicate is required before dispatch. The request expires after30 seconds and is bound to its original world and Farming armed-state; any observed blocker, unavailable context, another owner, stop/Jerry/world invalidation, disable or policy change clears it. No automatic reattempt follows a failed fresh task preflight. Queuing does not enable automatic Pest.

The real UI exposes pending status and the existing stop action while waiting. Closing settings starts at most one fresh bounded task with the original runtime's current target/tool/lease safeguards. `PestManualQueueTest` executes the actual task/session against native-boundary fixtures, and `UiModelTest` executes the actual queue/stop/visibility/status callbacks against compiled production UI classes without a world or native effects. Other manual-feature UI paths and delayed vanilla packets remain unchecked release work, not an implied extension of Pest's tests.
