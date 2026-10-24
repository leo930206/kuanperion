// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.List;
import java.util.Objects;
/** Captured together on the client thread; stale revisions cannot authorize clicks. */
public record MenuSnapshot(long worldId,int containerId,int stateId,String title,int containerSlots,List<Slot> slots){
 public record Slot(int index,ItemSnapshot item,boolean playerInventory){public Slot(int index,ItemSnapshot item){this(index,item,false);}public Slot{Objects.requireNonNull(item);if(index<0)throw new IllegalArgumentException("Negative slot");}}
 public MenuSnapshot{Objects.requireNonNull(title);slots=List.copyOf(slots);if(worldId<=0||containerId<0||stateId<0||containerSlots<0||slots.stream().map(Slot::index).distinct().count()!=slots.size())throw new IllegalArgumentException("Invalid container observation");}
 public Slot slot(int index){return slots.stream().filter(s->s.index()==index).findFirst().orElse(null);}
 public ItemSnapshot item(int index){var slot=slot(index);return slot==null?null:slot.item();}
}
