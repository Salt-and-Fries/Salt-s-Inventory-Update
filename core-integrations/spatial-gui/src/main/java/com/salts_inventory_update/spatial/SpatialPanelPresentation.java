package com.salts_inventory_update.spatial;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

/** Uniformly fits the entire surface. Window geometry and the upstream configuration are untouched. */
public final class SpatialPanelPresentation {
    private SpatialPanelPresentation() { }
    public record Rect(double left, double bottom, double right, double top) {
        public double width() { return right-left; }
        public double height() { return top-bottom; }
    }
    public record Fit(Matrix4f pose, float scale, Rect bounds, Rect available) { }
    public static Fit lastFit;
    private static final SpatialSurfaceTransition surface = new SpatialSurfaceTransition();
    public static Matrix4f lastPresentedView;

    /** Expand the same desktop into an exactly screen-aligned first-person rectangle. */
    public static Matrix4f present(Matrix4f spatialPose, float animation) {
        var camera=Minecraft.getInstance().gameRenderer.mainCamera();
        var view=new Matrix4f().rotateX((float)Math.toRadians(camera.xRot()))
            .rotateY((float)Math.toRadians(camera.yRot()+180));
        var spatialView=new Matrix4f(view).mul(spatialPose);
        float height=(float)(2*Math.tan(Math.toRadians(camera.getFov())/2));
        // Normalize projected size before retaining a pose. FOV animation must not resize
        // yesterday's source pose when it is reused by a later render pass or transition.
        var translation=spatialView.getTranslation(new Vector3f());
        spatialView.setTranslation(translation.x/height,translation.y/height,translation.z).scale(1/height);
        var mode=SpatialMovementTransition.closing()?SpatialSurfaceTransition.Mode.CLOSING:
            SpatialGuiIntegration.movement()?SpatialSurfaceTransition.Mode.MOVEMENT:SpatialSurfaceTransition.Mode.SPATIAL;
        var presented=surface.sample(spatialView,new Matrix4f().translation(0,0,-1),mode,
            SpatialMovementTransition.update(),SpatialMovementTransition.progress(),animation,System.nanoTime(),SpatialGUI.config.transitionDurationMs);
        translation=presented.getTranslation(new Vector3f());
        presented.setTranslation(translation.x*height,translation.y*height,translation.z).scale(height);
        lastPresentedView=new Matrix4f(presented);
        return view.invert().mul(presented);
    }

    public static void reset() { surface.reset(); lastPresentedView=null; }

    public static double curveDegrees(double configured) {
        return configured*(1-SpatialMovementTransition.blend());
    }

    public static Fit fit(Matrix4f source, boolean firstPerson, float aspect) {
        var mc=Minecraft.getInstance(); var camera=mc.gameRenderer.mainCamera();
        double yaw=Math.toRadians(camera.yRot()), pitch=Math.toRadians(camera.xRot());
        Vec3 forward=new Vec3(-Math.sin(yaw)*Math.cos(pitch),-Math.sin(pitch),Math.cos(yaw)*Math.cos(pitch));
        Vec3 right=new Vec3(-Math.cos(yaw),0,-Math.sin(yaw)), up=right.cross(forward);
        double ty=Math.tan(Math.toRadians(Math.clamp(camera.getFov(),10,150))/2), tx=ty*mc.getWindow().getScreenWidth()/Math.max(1,mc.getWindow().getScreenHeight());
        Rect available=new Rect(-.94,-.92,.94,.92);
        if(!firstPerson && mc.player!=null) {
            // Project an expanded avatar volume, including arms/head motion, into this actual camera.
            var box=mc.player.getBoundingBox().inflate(.28,.16,.28); Vec3 cameraPos=camera.position();
            double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
            for(int i=0;i<8;i++) {
                Vec3 p=new Vec3((i&1)==0?box.minX:box.maxX,(i&2)==0?box.minY:box.maxY,(i&4)==0?box.minZ:box.maxZ).subtract(cameraPos);
                double z=p.dot(forward);if(z<.05)continue;
                double x=p.dot(right)/(z*tx);min=Math.min(min,x);max=Math.max(max,x);
            }
            if(min<available.right && max>available.left) {
                double leftWidth=min-.06-available.left, rightWidth=available.right-max-.06;
                if(rightWidth>=leftWidth && rightWidth>.25) available=new Rect(Math.max(available.left,max+.06),available.bottom,available.right,available.top);
                else if(leftWidth>.25) available=new Rect(available.left,available.bottom,Math.min(available.right,min-.06),available.top);
            }
        }
        Vector3f originalCenter=source.transformPosition(new Vector3f());
        Vec3 sourceCenter=new Vec3(originalCenter.x,originalCenter.y,originalCenter.z);
        boolean curved=firstPerson && SpatialGUI.config.curvedScreenEnabled;
        double arc=curved?Math.toRadians(curveDegrees(SpatialGUI.config.curvedScreenArcDegrees)):0;
        if(arc<Math.toRadians(5)||arc>Math.toRadians(150))arc=0;
        Rect originalBounds=bounds(source,aspect,arc,forward,right,up,tx,ty);
        if(contains(available,originalBounds))return lastFit=new Fit(new Matrix4f(source),1,originalBounds,available);
        double depth=Math.max(.5,sourceCenter.dot(forward));
        float low=.0001f,high=1;
        Placement best=place(source,originalCenter,low,aspect,arc,available,depth,forward,right,up,tx,ty);
        // Find the largest fitting uniform scale, recentering the whole surface for each candidate.
        // This avoids shrinking repeatedly while the original center is still off-screen.
        for(int i=0;i<13;i++) {
            float candidate=(low+high)/2;
            var placed=place(source,originalCenter,candidate,aspect,arc,available,depth,forward,right,up,tx,ty);
            if(contains(available,placed.bounds)) {low=candidate;best=placed;} else high=candidate;
        }
        return lastFit=new Fit(best.pose,low,best.bounds,available);
    }
    private record Placement(Matrix4f pose,Rect bounds) { }
    private static boolean contains(Rect outer,Rect inner) {
        return inner!=null && inner.left>=outer.left+.001 && inner.right<=outer.right-.001
            && inner.bottom>=outer.bottom+.001 && inner.top<=outer.top-.001;
    }
    private static Placement place(Matrix4f source,Vector3f old,float scale,float aspect,double arc,Rect target,double depth,Vec3 forward,Vec3 right,Vec3 up,double tx,double ty) {
        double cx=(target.left+target.right)/2,cy=(target.bottom+target.top)/2;
        Vec3 center=forward.scale(depth).add(right.scale(cx*depth*tx)).add(up.scale(cy*depth*ty));
        Matrix4f pose=null;Rect box=null;
        for(int i=0;i<8;i++) {
            pose=transformed(source,old,center,scale);box=bounds(pose,aspect,arc,forward,right,up,tx,ty);
            if(box==null)break;
            double dx=cx-(box.left+box.right)/2,dy=cy-(box.bottom+box.top)/2;
            if(Math.abs(dx)+Math.abs(dy)<.00001)break;
            center=center.add(right.scale(dx*depth*tx)).add(up.scale(dy*depth*ty));
        }
        return new Placement(pose,box);
    }
    private static Matrix4f transformed(Matrix4f source,Vector3f old,Vec3 center,float scale) {
        return new Matrix4f().translation((float)center.x,(float)center.y,(float)center.z).scale(scale).translate(-old.x,-old.y,-old.z).mul(source);
    }
    private static Rect bounds(Matrix4f pose,float aspect,double arc,Vec3 forward,Vec3 right,Vec3 up,double tx,double ty) {
        double minX=Double.POSITIVE_INFINITY,minY=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX;
        int segments=arc>0?32:1;
        for(int i=0;i<=segments;i++)for(int side=0;side<2;side++) {
            double t=i/(double)segments-.5, x=aspect*t,z=0;
            if(arc>0) { double radius=aspect/arc,angle=t*arc;x=radius*Math.sin(angle);z=radius*(1-Math.cos(angle)); }
            Vector3f v=pose.transformPosition(new Vector3f((float)x,side==0?-.5f:.5f,(float)z));
            Vec3 p=new Vec3(v.x,v.y,v.z);double depth=p.dot(forward);if(depth<=.02)return null;
            double px=p.dot(right)/(depth*tx), py=p.dot(up)/(depth*ty);
            minX=Math.min(minX,px);maxX=Math.max(maxX,px);minY=Math.min(minY,py);maxY=Math.max(maxY,py);
        }
        return new Rect(minX,minY,maxX,maxY);
    }
}
