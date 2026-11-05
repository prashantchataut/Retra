package app.retra.core

import app.retra.engine.GbaKey

/**
 * Every action a physical button can trigger. [gbaKey] is the console bit, or 0 for app hotkeys.
 * Bindings are stored as `keyCode -> action name`; an empty map means "use the defaults".
 */
enum class PadAction(val label: String, val gbaKey: Int, val group: String) {
    A("A", GbaKey.A, "Buttons"),
    B("B", GbaKey.B, "Buttons"),
    L("L", GbaKey.L, "Buttons"),
    R("R", GbaKey.R, "Buttons"),
    START("Start", GbaKey.START, "Buttons"),
    SELECT("Select", GbaKey.SELECT, "Buttons"),
    UP("D-pad up", GbaKey.UP, "D-pad"),
    DOWN("D-pad down", GbaKey.DOWN, "D-pad"),
    LEFT("D-pad left", GbaKey.LEFT, "D-pad"),
    RIGHT("D-pad right", GbaKey.RIGHT, "D-pad"),
    FAST_FORWARD("Fast-forward (hold)", 0, "Shortcuts"),
    REWIND_HOLD("Rewind (hold)", 0, "Shortcuts"),
    REWIND_JUMP("Rewind a few seconds (tap)", 0, "Shortcuts"),
    MENU("Open menu", 0, "Shortcuts"),
}

object GamepadBindings {
    // android.view.KeyEvent constants, inlined so this stays a plain Kotlin object testable on the JVM.
    private const val BUTTON_A = 96
    private const val BUTTON_B = 97
    private const val BUTTON_X = 99
    private const val BUTTON_Y = 100
    private const val BUTTON_L1 = 102
    private const val BUTTON_R1 = 103
    private const val BUTTON_L2 = 104
    private const val BUTTON_R2 = 105
    private const val BUTTON_THUMBL = 106
    private const val BUTTON_START = 108
    private const val BUTTON_SELECT = 109
    private const val BUTTON_MODE = 110
    private const val DPAD_UP = 19
    private const val DPAD_DOWN = 20
    private const val DPAD_LEFT = 21
    private const val DPAD_RIGHT = 22

    /**
     * LABELS: the pad's A is the console's A (what Retra always did). POSITION: the console's A sits on the right
     * like on a real GBA, so the pad's east button is A and south is B (how Xbox/PlayStation pads usually feel).
     */
    enum class Preset(val label: String) { LABELS("Match labels"), POSITION("Match position") }

    fun preset(p: Preset): Map<Int, PadAction> {
        val common = mapOf(
            DPAD_UP to PadAction.UP, DPAD_DOWN to PadAction.DOWN, DPAD_LEFT to PadAction.LEFT, DPAD_RIGHT to PadAction.RIGHT,
            BUTTON_L1 to PadAction.L, BUTTON_R1 to PadAction.R,
            BUTTON_START to PadAction.START, BUTTON_SELECT to PadAction.SELECT,
            BUTTON_R2 to PadAction.FAST_FORWARD, BUTTON_L2 to PadAction.REWIND_HOLD,
            BUTTON_THUMBL to PadAction.REWIND_JUMP, BUTTON_MODE to PadAction.MENU,
        )
        return when (p) {
            Preset.LABELS -> common + mapOf(BUTTON_A to PadAction.A, BUTTON_B to PadAction.B, BUTTON_X to PadAction.B, BUTTON_Y to PadAction.A)
            // Android reports an Xbox pad's south button as BUTTON_A and its east button as BUTTON_B.
            Preset.POSITION -> common + mapOf(BUTTON_A to PadAction.B, BUTTON_B to PadAction.A, BUTTON_X to PadAction.B, BUTTON_Y to PadAction.A)
        }
    }

    val defaults: Map<Int, PadAction> get() = preset(Preset.LABELS)

    /** Keyboard keys that always work (not remappable here, never conflict with controllers). */
    private val keyboard = mapOf(
        45 to PadAction.L, // Q
        51 to PadAction.R, // W
        29 to PadAction.L, // A
        47 to PadAction.R, // S
        52 to PadAction.A, // X
        54 to PadAction.B, // Z
        66 to PadAction.START, // Enter
        62 to PadAction.SELECT, // Space
        61 to PadAction.FAST_FORWARD, // Tab
        68 to PadAction.REWIND_HOLD, // `
        111 to PadAction.MENU, // Esc
        82 to PadAction.MENU, // Menu
    )

    /** Decodes the stored map; unknown actions are dropped. Empty means the defaults. */
    fun resolve(stored: Map<Int, String>): Map<Int, PadAction> {
        if (stored.isEmpty()) return defaults
        val out = LinkedHashMap<Int, PadAction>()
        for ((k, v) in stored) PadAction.entries.firstOrNull { it.name == v }?.let { out[k] = it }
        return out.ifEmpty { defaults }
    }

    fun action(keyCode: Int, bindings: Map<Int, PadAction>): PadAction? {
        val arrows = when (keyCode) {
            DPAD_UP -> PadAction.UP; DPAD_DOWN -> PadAction.DOWN; DPAD_LEFT -> PadAction.LEFT; DPAD_RIGHT -> PadAction.RIGHT
            else -> null
        }
        return bindings[keyCode] ?: keyboard[keyCode] ?: arrows
    }

    /** Binds [keyCode] to [action]: a key does one thing, so it leaves any other action first. */
    fun bind(current: Map<Int, PadAction>, keyCode: Int, action: PadAction): Map<Int, PadAction> =
        current.filterKeys { it != keyCode } + (keyCode to action)

    fun unbind(current: Map<Int, PadAction>, action: PadAction): Map<Int, PadAction> = current.filterValues { it != action }

    fun keysFor(current: Map<Int, PadAction>, action: PadAction): List<Int> =
        current.filterValues { it == action }.keys.sorted()

    fun encode(map: Map<Int, PadAction>): Map<Int, String> = map.mapValues { it.value.name }

    /** Human label for a key code, e.g. 96 -> "Button A". */
    fun keyName(keyCode: Int): String = when (keyCode) {
        BUTTON_A -> "A (south)"
        BUTTON_B -> "B (east)"
        BUTTON_X -> "X (west)"
        BUTTON_Y -> "Y (north)"
        BUTTON_L1 -> "L1 / LB"
        BUTTON_R1 -> "R1 / RB"
        BUTTON_L2 -> "L2 / LT"
        BUTTON_R2 -> "R2 / RT"
        BUTTON_THUMBL -> "Left stick click"
        107 -> "Right stick click"
        BUTTON_START -> "Start"
        BUTTON_SELECT -> "Select / Back"
        BUTTON_MODE -> "Home / Guide"
        DPAD_UP -> "D-pad up"
        DPAD_DOWN -> "D-pad down"
        DPAD_LEFT -> "D-pad left"
        DPAD_RIGHT -> "D-pad right"
        else -> "Key $keyCode"
    }
}
