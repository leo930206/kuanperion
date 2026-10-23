package dev.kuan.kuanperion.ui.settings;
import java.util.List;
/** Independent toggles rendered in equal-width cells, without an implied master toggle. */
public final class ToggleRowSetting extends ToggleSetting {
    private final List<ToggleSetting> choices;
    public ToggleRowSetting(String name,List<ToggleSetting> choices) { super(name,()->false,v->{});this.choices=List.copyOf(choices); }
    public List<ToggleSetting> choices() { return choices; }
}
