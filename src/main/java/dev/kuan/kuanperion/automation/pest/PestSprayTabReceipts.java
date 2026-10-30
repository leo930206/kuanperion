// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import java.util.*;
/** Bounded applied Tab facts. Reading cached text cannot mint/refresh a receipt. No gameplay authority. */
public final class PestSprayTabReceipts {
 public static final long MAX_AGE=2_000_000_000L; // provisional conservative freshness ceiling, not a server cadence claim
 public record Scope(long world,Object level,Object player,int plot,long positionEpoch,long positionSequence){
  public Scope{if(world<=0||level==null||player==null||plot<1||plot>24||positionEpoch<=0||positionSequence<0)throw new IllegalArgumentException("Invalid spray Tab scope");}
  public boolean same(Scope other){return other!=null&&world==other.world&&level==other.level&&player==other.player&&plot==other.plot&&positionEpoch==other.positionEpoch&&positionSequence==other.positionSequence;}
 }
 public static final class Proof {
  private final Scope scope;private final Object row;private final List<String> entries;private final String field;private final long epoch,sequence,observedAt;
  private Proof(Scope scope,Object row,List<String> entries,String field,long epoch,long sequence,long now){this.scope=scope;this.row=row;this.entries=List.copyOf(entries);this.field=field;this.epoch=epoch;this.sequence=sequence;observedAt=now;}
  public Scope scope(){return scope;}public List<String> entries(){return entries;}public long epoch(){return epoch;}public long sequence(){return sequence;}public long observedAt(){return observedAt;}
  public boolean fresh(Scope current,long now){return scope.same(current)&&now-observedAt>=0&&now-observedAt<=MAX_AGE;}
 }
 private Scope scope;private Proof latest;private long epoch=1,sequence;
 /** Native caller must prove that this listed row's display-name write was actually applied by vanilla. */
 public void observe(Scope next,Object row,List<String> entries,int appliedEntry,long now){
  Objects.requireNonNull(next);if(scope==null||!scope.same(next)){clear();scope=next;}
  latest=null;sequence++;var field=PestSprayObservation.fieldEntry(entries);
  if(row==null||field.isEmpty()||field.getAsInt()!=appliedEntry)return;
  latest=new Proof(next,row,entries,entries.get(appliedEntry),epoch,sequence,now);
 }
 public Optional<Proof> current(Scope current,Object row,List<String> entries,long now){
  if(latest==null||row!=latest.row||!latest.fresh(current,now))return Optional.empty();var field=PestSprayObservation.fieldEntry(entries);
  return field.isPresent()&&latest.field.equals(entries.get(field.getAsInt()))?Optional.of(latest):Optional.empty();
 }
 public void clear(){scope=null;latest=null;sequence=0;epoch++;}
}
