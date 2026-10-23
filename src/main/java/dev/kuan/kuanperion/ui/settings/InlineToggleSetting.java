package dev.kuan.kuanperion.ui.settings;
import java.util.function.*;
/** One row, two independent controls: text on the left of a toggle. */
public final class InlineToggleSetting extends TextSetting {
 private final Supplier<Boolean> enabled;private final Consumer<Boolean> setter;
 public InlineToggleSetting(String name,String placeholder,Supplier<String> value,Consumer<String> commit,Supplier<Boolean> enabled,Consumer<Boolean> setter){super(name,placeholder,value,commit);
        numeric();this.enabled=enabled;this.setter=setter;}
 public boolean isOn(){return enabled.get();}
 public void toggle(){setter.accept(!isOn());}
}
