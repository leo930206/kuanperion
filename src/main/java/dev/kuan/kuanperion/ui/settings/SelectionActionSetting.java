package dev.kuan.kuanperion.ui.settings;
import java.util.function.*;
/** Target-count numeric editor and selection-start action on a shared row. */
public final class SelectionActionSetting extends SliderSetting {
 private final Runnable action;
 public SelectionActionSetting(String name,Supplier<Float> count,Consumer<Float> setter,Runnable action){super(name,1,5,count,setter);withDecimals(0);this.action=action;}
 public void start(){action.run();}
}
