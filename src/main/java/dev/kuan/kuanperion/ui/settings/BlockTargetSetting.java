package dev.kuan.kuanperion.ui.settings;
import java.util.function.*;
/** A real selected target only; order controls work on the existing target list. */
public final class BlockTargetSetting extends InfoSetting {
 private final Supplier<String> id;private final Runnable up,down,remove;private final Supplier<Boolean> canUp,canDown;
 public BlockTargetSetting(String name,Supplier<String> id,Supplier<String> display,Runnable up,Runnable down,Runnable remove,Supplier<Boolean> canUp,Supplier<Boolean> canDown){super(name,display);this.id=id;this.up=up;this.down=down;this.remove=remove;this.canUp=canUp;this.canDown=canDown;}
 public String displayLabel(){return getName()+": "+getValue();}
 public String blockId(){return id.get();}public boolean canMoveUp(){return canUp.get();}public boolean canMoveDown(){return canDown.get();}
 public void moveUp(){if(canMoveUp())up.run();}public void moveDown(){if(canMoveDown())down.run();}public void remove(){remove.run();}
}
