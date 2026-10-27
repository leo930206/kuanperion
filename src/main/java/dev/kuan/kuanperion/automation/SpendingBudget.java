// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.HashMap;
import java.util.Map;
/** Client-thread per-session coin budget. A reservation is not a purchase receipt. */
public final class SpendingBudget {
 public record Reservation(long token,long coins){}
 private final long limit;private long spent,held,next;private final Map<Long,Reservation> pending=new HashMap<>();
 public SpendingBudget(long limit){if(limit<0)throw new IllegalArgumentException("Negative budget");this.limit=limit;}
 public long limit(){return limit;}public long spent(){return spent;}public long available(){return limit-spent-held;}
 public Reservation reserve(long coins,long purse){if(coins<=0||coins>available()||purse<held||coins>purse-held)return null;var r=new Reservation(++next,coins);pending.put(r.token(),r);held+=coins;return r;}
 public boolean commit(Reservation receipt,long actualCoins){if(!valid(receipt)||actualCoins<0||actualCoins>receipt.coins())return false;pending.remove(receipt.token());held-=receipt.coins();spent+=actualCoins;return true;}
 public boolean release(Reservation reservation){if(!valid(reservation))return false;pending.remove(reservation.token());held-=reservation.coins();return true;}
 private boolean valid(Reservation r){return r!=null&&r.equals(pending.get(r.token()));}
}
