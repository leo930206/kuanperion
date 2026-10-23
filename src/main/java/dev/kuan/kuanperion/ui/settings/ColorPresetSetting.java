package dev.kuan.kuanperion.ui.settings;
import com.google.gson.JsonObject;
import dev.kuan.kuanperion.ui.theme.ColorPresetStore;
import java.io.IOException;
import java.util.function.*;
import java.util.List;

/** Fixed-slot menu-color action; caller supplies capture/apply to keep profiles out of gameplay state. */
public final class ColorPresetSetting extends InfoSetting {
 public enum SaveResult{SAVED,CONFIRM}
 private final int slot;
 private final ColorPresetStore store;
 private final Supplier<JsonObject> capture;
 private final Consumer<JsonObject> apply;
 private long confirmUntil;
 private final List<String> previewOrder;
 private List<Integer> preview=List.of();
 public ColorPresetSetting(int slot,ColorPresetStore store,Supplier<JsonObject> capture,Consumer<JsonObject> apply){
  this(slot,store,capture,apply,dev.kuan.kuanperion.ui.theme.SkyThemePalette.keys().stream().sorted().toList());
 }
 public ColorPresetSetting(int slot,ColorPresetStore store,Supplier<JsonObject> capture,Consumer<JsonObject> apply,List<String> previewOrder){
  super("配色收藏",()->"");store.occupied(slot);this.slot=slot;this.store=store;this.capture=capture;this.apply=apply;
  this.previewOrder=List.copyOf(previewOrder);refreshPreview();
 }
 private void refreshPreview(){
  try{
   JsonObject colors=store.read(slot);
   preview=colors==null?List.of():previewOrder.stream().filter(colors::has).map(key->(int)Long.parseLong(colors.get(key).getAsString(),16)).toList();
  }catch(IOException|RuntimeException error){preview=List.of();}
 }
 public List<Integer> previewColors(){return preview;}
 @Override public boolean isVisible(){return occupied();}
 public void delete()throws IOException{store.delete(slot);confirmUntil=0;refreshPreview();}
 public boolean occupied(){return store.occupied(slot);}
 public String saveLabel(long now){return now<confirmUntil?"確認儲存":"儲存";}
 public SaveResult save(long now)throws IOException{
  if(occupied() && now>=confirmUntil){confirmUntil=now+5000;return SaveResult.CONFIRM;}
  store.save(slot,capture.get(),occupied());confirmUntil=0;refreshPreview();return SaveResult.SAVED;
 }
 public boolean load()throws IOException{
  JsonObject colors=store.read(slot);if(colors==null)return false;
  apply.accept(colors);confirmUntil=0;return true;
 }
 @Override public String getDescription(){return "點擊已儲存配色立即載入；右側儲存目前顏色，覆寫須在 5 秒內再次確認。";}
 @Override public String getValue(){return "";}
}
