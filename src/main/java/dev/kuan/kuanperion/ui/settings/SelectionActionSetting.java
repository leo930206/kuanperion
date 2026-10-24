package dev.kuan.kuanperion.ui.settings;
import java.util.function.*;
/** New target count, dynamically bounded by available saved-target slots. */
public final class SelectionActionSetting extends SliderSetting {
 private final Runnable action;
 private final IntSupplier available;
 public SelectionActionSetting(String name,Supplier<Float> count,Consumer<Float> setter,Runnable action){this(name,count,setter,action,()->10);}
 public SelectionActionSetting(String name,Supplier<Float> count,Consumer<Float> setter,Runnable action,IntSupplier available){super(name,0,10,count,setter);withDecimals(0);this.action=action;this.available=available;}
 @Override public float getMin(){return canStart()?1:0;}
 @Override public float getMax(){return Math.max(0,Math.min(10,available.getAsInt()));}
 @Override public float getValue(){return Math.max(getMin(),Math.min(getMax(),super.getValue()));}
 @Override public void setValue(float value){if(Float.isFinite(value))super.setValue(Math.round(Math.max(getMin(),Math.min(getMax(),value))));}
 @Override public String getDescription(){return canStart()?"可新增 1～"+(int)getMax()+" 種方塊；新選種類排最前，保留原清單（最多 10 種）。":"已選滿 10 種；請先刪除不需要的種類，再新增方塊。";}
 public boolean canStart(){return getMax()>0;}
 public void start(){if(canStart())action.run();}
}
