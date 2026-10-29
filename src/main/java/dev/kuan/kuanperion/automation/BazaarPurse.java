// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.*;import java.util.regex.*;import java.math.*;
/** Bounded exact sidebar observation, not a configured budget or API estimate. U-BZ-PRICE provisional grammar. */
public final class BazaarPurse {
 private static final Pattern VALUE=Pattern.compile("(?:Purse|Piggy): ((?:0|[1-9][0-9]*|[1-9][0-9]{0,2}(?:,[0-9]{3})+)(?:\\.[0-9]{1,2})?)");
 public static OptionalLong read(List<String> lines){
  if(lines==null||lines.size()>128)return OptionalLong.empty();Long found=null;
  for(String raw:lines){if(raw==null||raw.length()>256)return OptionalLong.empty();String s=raw.replaceAll("(?i)§[0-9a-fk-orx]","").trim();if(!s.startsWith("Purse:")&&!s.startsWith("Piggy:"))continue;var m=VALUE.matcher(s);if(found!=null||!m.matches())return OptionalLong.empty();
   try{found=new BigDecimal(m.group(1).replace(",","")).setScale(0,RoundingMode.FLOOR).longValueExact();if(found>1_000_000_000_000L)return OptionalLong.empty();}catch(ArithmeticException invalid){return OptionalLong.empty();}
  }return found==null?OptionalLong.empty():OptionalLong.of(found);
 }
 private BazaarPurse(){}
}
