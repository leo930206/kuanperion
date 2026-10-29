// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.composter;
import dev.kuan.kuanperion.automation.*;import java.util.*;
/** U-COMP: source main39/41 -> detail11 -> main. Detail labels are explicit provisional local grammar. */
public final class ComposterSacksView {
 public record Button(MenuContract contract,MenuContract.Ticket ticket){public int slot(){return ticket.slot();}}
 public static Optional<Button> open(MenuSnapshot menu,ComposterObservation.Kind kind,long remaining){
  if(kind==null||!BazaarNavigationView.complete(menu))return Optional.empty();var seen=ComposterObservation.inspect(menu).orElse(null);if(seen==null)return Optional.empty();
  var resource=kind==ComposterObservation.Kind.ORGANIC?seen.organic():seen.fuel();int slot=kind==ComposterObservation.Kind.ORGANIC?seen.organicSlot():seen.fuelSlot();
  if(slot!=(kind==ComposterObservation.Kind.ORGANIC?39:41)||resource.current()>=resource.capacity()||remaining<resource.capacity())return Optional.empty();return button(menu,slot);
 }
 public static Optional<Button> fill(MenuSnapshot menu,ComposterObservation.Kind kind){
  if(kind==null||!BazaarNavigationView.complete(menu))return Optional.empty();String label=kind==ComposterObservation.Kind.ORGANIC?"Organic Matter":"Fuel";
  if(!Set.of(label,"Composter "+label).contains(menu.title()))return Optional.empty();var item=menu.item(11);
  if(item.count()!=1||item.protectedItem()||item.identified()||!item.uuid().isEmpty()||item.minecraftId().equals("minecraft:barrier")||!Set.of("Fill from Sacks","Insert from Sacks").contains(item.name())||item.lore().stream().anyMatch(s->s.toLowerCase(Locale.ROOT).matches(".*\\b(?:cost|coins?|bits|gems|confirm|warning)\\b.*")))return Optional.empty();
  for(int i=0;i<menu.containerSlots();i++)if(i!=11&&Set.of("Fill from Sacks","Insert from Sacks").contains(menu.item(i).name()))return Optional.empty();return button(menu,11);
 }
 private static Optional<Button> button(MenuSnapshot m,int slot){var c=new MenuContract(Set.of(m.title()),m.containerSlots());return Optional.of(new Button(c,c.plan(m,slot,MenuContract.Click.PICKUP)));}
 private ComposterSacksView(){}
}
