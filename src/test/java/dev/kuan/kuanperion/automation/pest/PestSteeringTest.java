package dev.kuan.kuanperion.automation.pest;
public final class PestSteeringTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
  var origin=new PestTargets.Point(0,70,0);var east=new PestTargets.Point(10,70,0);
  var first=PestSteering.toward(origin,east,0,0,12);
  check(first.yaw()==-12&&!first.forward()&&!first.up()&&!first.down(),"bounded turn before forward");
  var aligned=PestSteering.toward(origin,east,-90,0,12);check(aligned.forward()&&aligned.yaw()==-90,"east direction");
  var above=PestSteering.toward(origin,new PestTargets.Point(0,73,0),90,0,12);check(!above.forward()&&above.up()&&!above.down(),"pure vertical never walks forward");
  check(PestSteering.rotate(179,-179,12,true)==181,"short wrapped yaw path");
  var zero=PestSteering.toward(origin,origin,34,0,12);check(!zero.forward()&&!zero.up()&&!zero.down(),"no input at destination");
  var braking=PestSteering.toward(origin,new PestTargets.Point(0,70,.4),new PestTargets.Point(0,.3,.5),0,0,12);check(!braking.forward()&&braking.back()&&braking.down(),"counter-steer excess inertia before overshooting the waypoint");
  try{PestSteering.rotate(Float.NaN,1,12,true);throw new AssertionError("NaN accepted");}catch(IllegalArgumentException expected){}
  for(double acceleration:new double[]{.02,.04,.08,.12})for(double damping:new double[]{.6,.8,.91}){
   double x=0,z=0,y=70,vx=0,vz=0,vy=0;float yaw=0;boolean arrived=false;
   for(int tick=0;tick<1200;tick++){
    var pose=new PestTargets.Point(x,y,z);var goal=new PestTargets.Point(0,72,12);
    if(pose.distanceSquared(goal)<.175*.175&&vx*vx+vz*vz+vy*vy<.000225){arrived=true;break;}
    var input=PestSteering.toward(pose,goal,new PestTargets.Point(vx,vy,vz),yaw,0,12);
    yaw=input.yaw();double direction=(input.forward()?1:0)-(input.back()?1:0);
    vx=(vx-acceleration*direction*Math.sin(Math.toRadians(yaw)))*damping;
    vz=(vz+acceleration*direction*Math.cos(Math.toRadians(yaw)))*damping;
    // Player.travel in pinned26.2 applies 0.6 vertical flight drag; horizontal fixtures vary independently.
    vy=(vy+acceleration*((input.up()?1:0)-(input.down()?1:0)))*.6;
    x+=vx;z+=vz;y+=vy;
   }
   check(arrived,"key-only convergence across conservative inertia fixtures: "+acceleration+"/"+damping+" end="+x+","+y+","+z);
  }
  System.out.println("PestSteeringTest: PASS (bounded wrap-aware rotation, alignment-before-motion, independent vertical input, finite validation)");
 }
}
