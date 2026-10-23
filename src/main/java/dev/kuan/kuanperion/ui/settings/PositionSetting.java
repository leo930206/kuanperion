// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui.settings;

import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingType;
import dev.kuan.kuanperion.util.UiLang;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PositionSetting
implements Setting {
    private final String name;
    private final String rawName;
    private final Supplier<Double> xGetter;
    private final Consumer<Double> xSetter;
    private final Supplier<Double> yGetter;
    private final Consumer<Double> ySetter;
    private final Supplier<Double> zGetter;
    private final Consumer<Double> zSetter;
    private final Supplier<Boolean> highlightGetter;
    private final Consumer<Boolean> highlightSetter;
    private final Runnable captureAction;
    private final List<ActionButton> actionButtons = new ArrayList<ActionButton>();
    private Supplier<Boolean> visibility = () -> true;

    public PositionSetting(String name, Supplier<Double> xGetter, Consumer<Double> xSetter, Supplier<Double> yGetter, Consumer<Double> ySetter, Supplier<Double> zGetter, Consumer<Double> zSetter, Supplier<Boolean> highlightGetter, Consumer<Boolean> highlightSetter, Runnable captureAction) {
        this.rawName = name;
        this.name = UiLang.localize(name);
        this.xGetter = xGetter;
        this.xSetter = xSetter;
        this.yGetter = yGetter;
        this.ySetter = ySetter;
        this.zGetter = zGetter;
        this.zSetter = zSetter;
        this.highlightGetter = highlightGetter;
        this.highlightSetter = highlightSetter;
        this.captureAction = captureAction;
    }

    public double getX() {
        return this.xGetter.get();
    }

    public void setX(double v) {
        this.xSetter.accept(v);
    }

    public double getY() {
        return this.yGetter.get();
    }

    public void setY(double v) {
        this.ySetter.accept(v);
    }

    public double getZ() {
        return this.zGetter.get();
    }

    public void setZ(double v) {
        this.zSetter.accept(v);
    }

    public boolean isHighlighted() {
        return this.highlightGetter.get();
    }

    public void setHighlighted(boolean v) {
        this.highlightSetter.accept(v);
    }

    public void capture() {
        this.captureAction.run();
    }

    public List<ActionButton> getActionButtons() {
        return this.actionButtons;
    }

    public PositionSetting addActionButton(String label, Runnable action, Supplier<Boolean> enabled) {
        this.actionButtons.add(new ActionButton(label, action, enabled));
        return this;
    }

    public PositionSetting visibleWhen(Supplier<Boolean> condition) {
        this.visibility = condition;
        return this;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getRawName() {
        return this.rawName;
    }

    @Override
    public SettingType getType() {
        return SettingType.POSITION;
    }

    @Override
    public boolean isVisible() {
        return this.visibility.get();
    }

    public record ActionButton(String label, Runnable action, Supplier<Boolean> enabled) {
        public boolean isEnabled() {
            return this.enabled == null || this.enabled.get() != false;
        }

        public void execute() {
            if (this.isEnabled() && this.action != null) {
                this.action.run();
            }
        }
    }
}
