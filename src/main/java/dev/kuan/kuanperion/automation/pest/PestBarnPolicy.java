// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
/** Separate opt-in source-style Barn desk route; no implicit coordinates, travel budget, NPC or setspawn authority. */
public record PestBarnPolicy(boolean enabled,PestTargets.Point desk){
 public PestBarnPolicy{if(desk!=null&&(!PestPlot.of(0).contains(desk.x(),desk.z())||Math.abs(desk.y())>=2048))throw new IllegalArgumentException("Pest exchange desk must be a finite Barn position");}
 public static PestBarnPolicy defaults(){return new PestBarnPolicy(false,null);}
 public boolean configured(){return enabled&&desk!=null;}
 public PestBarnPolicy withEnabled(boolean b){return new PestBarnPolicy(b,desk);}
 public String deskText(){return desk==null?"":java.math.BigDecimal.valueOf(desk.x()).toPlainString()+" "+java.math.BigDecimal.valueOf(desk.y()).toPlainString()+" "+java.math.BigDecimal.valueOf(desk.z()).toPlainString();}
 public PestBarnPolicy withDeskText(String text){
  if(text==null||text.length()>96)throw new IllegalArgumentException("請輸入 Barn 桌面 X Y Z 座標，留空清除");
  if(text.isBlank())return withDesk(null);var parts=text.strip().split("[\\s,]+",-1);
  if(parts.length!=3)throw new IllegalArgumentException("請輸入三個 Barn 座標 X Y Z");
  double[] xyz=new double[3];for(int i=0;i<3;i++){if(!parts[i].matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)"))throw new IllegalArgumentException("座標必須為有限十進位數字");xyz[i]=Double.parseDouble(parts[i]);}
  return withDesk(new PestTargets.Point(xyz[0],xyz[1],xyz[2]));
 }
 public PestBarnPolicy withDesk(PestTargets.Point p){return new PestBarnPolicy(enabled,p);}
}
