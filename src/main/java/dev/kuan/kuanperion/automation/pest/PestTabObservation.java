// SPDX-License-Identifier: GPL-3.0-only
// Pest field concepts examined in Aether PestTabSnapshot; strict scoped parser rewritten locally.
package dev.kuan.kuanperion.automation.pest;

import java.util.*;
import java.util.regex.Pattern;

/** Read-only facts. Unknown is not zero, READY, an active bonus or an authorized destination. */
public record PestTabObservation(int alive,Set<Integer> plots,boolean plotsKnown,Cooldown cooldown,Bonus bonus) {
    public enum Bonus {UNKNOWN,ACTIVE,INACTIVE}
    public enum CooldownState {UNKNOWN,READY,WAITING,MAX_PESTS}
    public record Cooldown(CooldownState state,int seconds) {
        public Cooldown {Objects.requireNonNull(state);if(state==CooldownState.WAITING?(seconds<1||seconds>5999):seconds!=0)throw new IllegalArgumentException("Invalid cooldown");}
    }
    private static final Cooldown UNKNOWN=new Cooldown(CooldownState.UNKNOWN,0);
    private static final Pattern COLORS=Pattern.compile("§[0-9a-fk-orx]",Pattern.CASE_INSENSITIVE);
    private static final Pattern HEADER=Pattern.compile("(?i)^Pests\\s*:\\s*(.*)$");
    private static final Pattern TIMER=Pattern.compile("(?i)^(?:(0|[1-9][0-9]?)m)?\\s*(?:(0|[1-9][0-9]?)s)?$");
    public PestTabObservation {
        plots=Set.copyOf(plots);Objects.requireNonNull(cooldown);Objects.requireNonNull(bonus);
        if(alive< -1||alive>999||plots.size()>24||plots.stream().anyMatch(p->p<1||p>24)||!plotsKnown&&!plots.isEmpty())throw new IllegalArgumentException("Invalid Pest observation");
    }
    public static PestTabObservation unknown(){return new PestTabObservation(-1,Set.of(),false,UNKNOWN,Bonus.UNKNOWN);}
    public static PestTabObservation parse(List<String> entries) {
        if(entries==null||entries.size()>128)return unknown();
        int count=-1,total=0,lines=0;boolean inPests=false,badCount=false,badPlots=false,plotsSeen=false,cooldownSeen=false,badCooldown=false,bonusSeen=false,badBonus=false;
        Set<Integer> plots=Set.of();Cooldown cooldown=UNKNOWN;Bonus bonus=Bonus.UNKNOWN;
        for(String entry:entries){
            if(entry==null||entry.length()>4096||(total+=entry.length())>65536)return unknown();
            for(String raw:entry.split("\\R",-1)){
                if(++lines>256||raw.length()>512)return unknown();
                String line=COLORS.matcher(raw).replaceAll("").replaceAll("[\\p{Zs}]"," ").replaceAll("\\p{Cf}","").strip();
                if(line.isEmpty())continue;
                var heading=HEADER.matcher(line);
                if(heading.matches()){
                    inPests=true;String value=heading.group(1);if(value.isEmpty())continue;
                    int next=count(value);if(next<0||count>=0&&next!=count)badCount=true;else count=next;continue;
                }
                if(!inPests)continue;
                int colon=line.indexOf(':');if(colon<0){inPests=false;continue;}
                String field=line.substring(0,colon).strip().toLowerCase(Locale.ROOT),value=line.substring(colon+1).strip();
                switch(field){
                    case "alive" -> {int next=count(value.replaceFirst("(?i)^[x×]\\s*",""));if(next<0||count>=0&&next!=count)badCount=true;else count=next;}
                    case "plot","plots" -> {
                        var next=plots(value);if(next==null||plotsSeen&&!next.equals(plots))badPlots=true;else plots=next;plotsSeen=true;
                    }
                    case "cooldown" -> {var next=cooldown(value);if(cooldownSeen&&!cooldown.equals(next))badCooldown=true;cooldown=next;cooldownSeen=true;}
                    case "bonus" -> {var next=value.equalsIgnoreCase("ACTIVE")?Bonus.ACTIVE:value.equalsIgnoreCase("INACTIVE")?Bonus.INACTIVE:Bonus.UNKNOWN;if(bonusSeen&&bonus!=next)badBonus=true;bonus=next;bonusSeen=true;}
                    case "spray","repellent" -> { /* Known Pest fields that do not authorize any action here. */ }
                    default -> inPests=false;
                }
            }
        }
        boolean knownPlots=plotsSeen&&!badPlots;
        return new PestTabObservation(badCount?-1:count,knownPlots?plots:Set.of(),knownPlots,badCooldown?UNKNOWN:cooldown,badBonus?Bonus.UNKNOWN:bonus);
    }
    private static int count(String input){
        String value=input;if(value.startsWith("(")&&value.endsWith(")"))value=value.substring(1,value.length()-1).strip();
        return value.matches("0|[1-9][0-9]{0,2}")?Integer.parseInt(value):-1;
    }
    private static Set<Integer> plots(String value){
        var result=new LinkedHashSet<Integer>();String[] parts=value.split(",",-1);if(parts.length<1||parts.length>24)return null;
        for(String part:parts){String id=part.strip();if(id.startsWith("#"))id=id.substring(1);if(!id.matches("[1-9]|1[0-9]|2[0-4]"))return null;if(!result.add(Integer.parseInt(id)))return null;}
        return Set.copyOf(result);
    }
    private static Cooldown cooldown(String value){
        if(value.equalsIgnoreCase("READY"))return new Cooldown(CooldownState.READY,0);
        if(value.equalsIgnoreCase("MAX PEST")||value.equalsIgnoreCase("MAX PESTS"))return new Cooldown(CooldownState.MAX_PESTS,0);
        var timer=TIMER.matcher(value);if(!timer.matches()||timer.group(1)==null&&timer.group(2)==null)return UNKNOWN;
        int minutes=timer.group(1)==null?0:Integer.parseInt(timer.group(1)),seconds=timer.group(2)==null?0:Integer.parseInt(timer.group(2));
        int total=minutes*60+seconds;if(seconds>59||total<1||total>5999)return UNKNOWN;
        return new Cooldown(CooldownState.WAITING,total);
    }
}
