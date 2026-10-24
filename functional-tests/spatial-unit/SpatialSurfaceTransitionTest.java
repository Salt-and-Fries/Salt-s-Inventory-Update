package com.salts_inventory_update.spatial;

import org.joml.Matrix4f;

/** Deterministic continuity contracts; runs without a Minecraft client or optional mod. */
public class SpatialSurfaceTransitionTest {
    private static final Matrix4f NORMAL=new Matrix4f().translation(0,0,-1);
    private static final Matrix4f SPATIAL=new Matrix4f().translation(.8f,.15f,-2).rotateY(.35f).scale(.65f);
    private static final SpatialSurfaceTransition.Mode S=SpatialSurfaceTransition.Mode.SPATIAL,
        M=SpatialSurfaceTransition.Mode.MOVEMENT,C=SpatialSurfaceTransition.Mode.CLOSING;
    private static int assertions;
    private static void equal(Matrix4f a,Matrix4f b,String message){assertions++;if(!a.equals(b,.00001f))throw new AssertionError(message);}
    private static Matrix4f sample(SpatialSurfaceTransition t,Matrix4f target,SpatialSurfaceTransition.Mode mode,int revision,double progress,float animation,long ms){
        return t.sample(target,NORMAL,mode,revision,progress,animation,ms*1_000_000,350);
    }
    public static void main(String[] args) {
        var t=new SpatialSurfaceTransition();
        equal(sample(t,SPATIAL,S,1,0,.8f,1),new Matrix4f(NORMAL).scale(.8f),"Opening must begin at the current normal presentation");
        Matrix4f prior=null;
        for(long ms=17;ms<=1201;ms+=16)prior=sample(t,SPATIAL,S,1,1,1,ms);
        assertions++;if(!prior.equals(SPATIAL,.001f))throw new AssertionError("Opening must settle within a fraction of a screen pixel of the fitted pose");
        equal(sample(t,SPATIAL,M,2,0,1,1201),prior,"Alt press must start at the last displayed pose");
        var halfway=sample(t,SPATIAL,M,2,.5,1,1360);
        equal(sample(t,SPATIAL,S,3,0,1,1360),halfway,"Reversing Alt must not jump");
        var shifted=new Matrix4f(SPATIAL).translate(3,0,0);
        equal(sample(t,shifted,S,3,.2,1,1360),halfway,"A fit changing sides must not teleport on the same timestamp");
        var closed=sample(t,shifted,C,4,0,1,1360);
        equal(closed,halfway,"Closing must retain the last fitted placement");
        var shrunk=sample(t,shifted,C,4,.5,.6f,1450);
        equal(shrunk,new Matrix4f(closed).scale(.6f),"Close scale must stay centered on the captured panel");
        equal(sample(t,SPATIAL,S,5,0,.8f,1450),shrunk,"Reopening during close must continue from the visible panel");
        sample(t,SPATIAL,M,6,0,1,1500);
        equal(sample(t,SPATIAL,M,6,1,1,1850),NORMAL,"Settled movement must exactly align with the physical screen");
        equal(sample(t,SPATIAL,S,7,0,1,1850),NORMAL,"Alt release must begin at the normal desktop");
        t.reset();
        equal(sample(t,SPATIAL,S,8,0,.8f,2000),new Matrix4f(NORMAL).scale(.8f),"A later independent session must not inherit the old panel");
        var near=new SpatialSurfaceTransition();var far=new SpatialSurfaceTransition();
        sample(near,SPATIAL,S,1,0,1,1);sample(far,SPATIAL,S,1,0,1,1);
        var a=sample(near,SPATIAL,S,1,.1,1,17);
        var b=sample(far,new Matrix4f().scale(100).mul(SPATIAL),S,1,.1,1,17);
        assertions++;if(Math.abs(a.m30()/a.m32()-b.m30()/b.m32())>.00001)throw new AssertionError("Depth must not accelerate projected panel motion");
        assertions++;if(Math.abs(a.getScale(new org.joml.Vector3f()).y/a.m32()-b.getScale(new org.joml.Vector3f()).y/b.m32())>.00001)throw new AssertionError("Depth must not accelerate projected panel resizing");
        System.out.println("PASS: "+assertions+" spatial presentation continuity assertions");
    }
}
