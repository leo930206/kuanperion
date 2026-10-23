package dev.kuan.kuanperion.ui.settings;
import com.google.gson.JsonObject;
import dev.kuan.kuanperion.ui.theme.ColorPresetStore;
import java.io.IOException;
import java.util.function.*;

/** Fixed-slot menu-color action; caller supplies capture/apply to keep profiles out of gameplay state. */
public final class ColorPresetSetting extends InfoSetting {
 public enum SaveResult{SAVED,CONFIRM}
 private final int slot;
 private final ColorPresetStore store;
 private final Supplier<JsonObject> capture;
 private final Consumer<JsonObject> apply;
 private long confirmUntil;
 public ColorPresetSetting(int slot,ColorPresetStore store,Supplier<JsonObject> capture,Consumer<JsonObject> apply){
  super("配色 "+slot,()->"");store.occupied(slot);this.slot=slot;this.store=store;this.capture=capture;this.apply=apply;
 }
 public boolean occupied(){return store.occupied(slot);}
 public String saveLabel(long now){return now<confirmUntil?"確認覆寫":occupied()?"覆寫目前配色":"儲存目前配色";}
 public SaveResult save(long now)throws IOException{
  if(occupied() && now>=confirmUntil){confirmUntil=now+5000;return SaveResult.CONFIRM;}
  store.save(slot,capture.get(),occupied());confirmUntil=0;return SaveResult.SAVED;
 }
 public boolean load()throws IOException{
  JsonObject colors=store.read(slot);if(colors==null)return false;
  apply.accept(colors);confirmUntil=0;return true;
 }
 @Override public String getDescription(){return "點擊已儲存配色立即載入；右側儲存目前顏色，覆寫須在 5 秒內再次確認。";}
 @Override public String getValue(){return occupied()?"已儲存 · 點擊此列載入":"尚未儲存 · 點右側儲存";}
}
