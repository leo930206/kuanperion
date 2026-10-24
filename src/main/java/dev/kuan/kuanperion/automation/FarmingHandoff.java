// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
/** A hold is not an enable flag. Only the same manually armed session may continue. */
public final class FarmingHandoff {
 public record Token(long nonce,long worldId,long farmingSession){}
 private long generation;private Token active;
 public Token acquire(long world,long session,boolean armed){if(active!=null||!armed||world<=0||session<=0)return null;return active=new Token(++generation,world,session);}
 public boolean held(){return active!=null;}
 public boolean finish(Token token,long world,long session,boolean armed){if(token==null||!token.equals(active))return false;active=null;return armed&&token.worldId()==world&&token.farmingSession()==session;}
 public void invalidate(){active=null;generation++;}
}
