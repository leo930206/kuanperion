// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.Set;
import java.util.regex.Pattern;
/** Exact title/shape and same-world/container/revision/item preconditions; never guesses slot numbers. */
public record MenuContract(Set<String> titles,int containerSlots){
 public enum Click {PICKUP,QUICK_MOVE,THROW}
 public record Ticket(MenuSnapshot observed,int slot,Click click){}
 private static final Pattern COINS=Pattern.compile("Cost: ([0-9]+|[1-9][0-9]{0,2}(?:,[0-9]{3})+) Coins");
 public MenuContract{titles=Set.copyOf(titles);if(titles.isEmpty()||containerSlots<1)throw new IllegalArgumentException("Empty menu contract");}
 public boolean matches(MenuSnapshot menu){return menu!=null&&menu.containerSlots()==containerSlots&&titles.contains(menu.title());}
 public Ticket plan(MenuSnapshot menu,int slot,Click click){return matches(menu)&&menu.item(slot)!=null&&menu.item(slot).count()>0&&click!=null?new Ticket(menu,slot,click):null;}
 public boolean revalidate(Ticket ticket,MenuSnapshot current){if(ticket==null||!matches(current))return false;var old=ticket.observed();return matches(old)&&old.worldId()==current.worldId()&&old.containerId()==current.containerId()&&old.stateId()==current.stateId()&&old.item(ticket.slot())!=null&&old.slot(ticket.slot()).equals(current.slot(ticket.slot()));}
 public static long exactCoins(String line){var m=COINS.matcher(line);if(!m.matches())return -1;try{return Long.parseLong(m.group(1).replace(",",""));}catch(NumberFormatException overflow){return -1;}}
}
