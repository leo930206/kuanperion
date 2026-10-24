package dev.kuan.kuanperion;
import java.util.Set;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
public final class AutomationPositionViewTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[]args){
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();runFixtures();
 }
 static void runFixtures(){
  var change=new PositionMoveRotation(new Vec3(12.5,75,-30.25),Vec3.ZERO,90,0);var packet=new ClientboundPlayerPositionPacket(-7,change,Set.of(Relative.Y_ROT,Relative.DELTA_X));
  var point=AutomationPositionView.absolute(packet);check(point!=null&&point.x()==12.5&&point.y()==75&&point.z()==-30.25,"absolute XYZ remains independent of rotation/velocity-relative flags");
  for(var axis:Set.of(Relative.X,Relative.Y,Relative.Z))check(AutomationPositionView.absolute(new ClientboundPlayerPositionPacket(8,change,Set.of(axis)))==null,"relative "+axis+" cannot be mistaken for an absolute server destination");
  check(AutomationPositionView.absolute(null)==null&&AutomationPositionView.absolute(new ClientboundPlayerPositionPacket(8,null,Set.of()))==null,"incomplete packet not position proof");
  var invalid=new PositionMoveRotation(new Vec3(Double.NaN,75,0),Vec3.ZERO,0,0);check(AutomationPositionView.absolute(new ClientboundPlayerPositionPacket(9,invalid,Set.of()))==null,"native nonfinite position rejected");
  check(packet.change().equals(change)&&packet.relatives().equals(Set.of(Relative.Y_ROT,Relative.DELTA_X)),"read-only packet projection");
  System.out.println("AutomationPositionViewTest: PASS (pinned26.2 native packets, absolute XYZ only, rotation/delta flags, malformed/nonfinite and immutability)");
 }
}
