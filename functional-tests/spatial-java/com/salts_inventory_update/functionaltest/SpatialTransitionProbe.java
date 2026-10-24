package com.salts_inventory_update.functionaltest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;
import org.tastytrash.spatialGUI.SpatialGUI;

/** QA-only measurements of the actual submitted surface, including the upstream close pass. */
public final class SpatialTransitionProbe {
    private static boolean enabled;
    private static long frame = -1;
    private static double[] lastObserved;
    private static final List<double[]> samples = new ArrayList<>();
    public static void begin() {
        samples.clear();enabled=true;frame=-1;
        if(lastObserved!=null&&System.nanoTime()/1e9-lastObserved[0]<.2)samples.add(lastObserved.clone());
    }
    public static List<double[]> finish() { enabled=false; return new ArrayList<>(samples); }
    public static void observe(Matrix4f world) {
        long current=org.tastytrash.spatialGUI.render.SpatialGUIRenderer.frameCounter();
        if(frame==current)return;
        frame=current;
        var mc=Minecraft.getInstance();var camera=mc.gameRenderer.mainCamera();
        var renderer=SpatialGUIClient.renderer();
        if(!renderer.shouldCapture()&&!renderer.getInventoryRenderer().isClosing())return;
        var pose=new Matrix4f().rotateX((float)Math.toRadians(camera.xRot()))
            .rotateY((float)Math.toRadians(camera.yRot()+180)).mul(world);
        double ty=Math.tan(Math.toRadians(camera.getFov())/2),aspect=(double)mc.getWindow().getWidth()/mc.getWindow().getHeight();
        double[] sample=new double[12];sample[0]=System.nanoTime()/1e9;
        sample[1]=renderer.getInventoryRenderer().isClosing()?2:0;
        sample[2]=camera.position().distanceTo(mc.player.getEyePosition());sample[3]=camera.getFov();
        boolean first=sample[1]==2?renderer.getInventoryRenderer().isClosingFirstPerson():SpatialGUIClient.getEffectiveFirstPersonMode();
        double arc=first&&SpatialGUI.config.curvedScreenEnabled?Math.toRadians(Math.round(com.salts_inventory_update.spatial.SpatialPanelPresentation.curveDegrees(SpatialGUI.config.curvedScreenArcDegrees))):0;
        if(arc<Math.toRadians(5)||arc>Math.toRadians(150))arc=0;
        for(int i=0;i<4;i++){
            double x=(i%2==0?-1:1)*aspect/2,z=0;
            if(arc>0){double angle=(i%2==0?-1:1)*arc/2;x=aspect/arc*Math.sin(angle);z=aspect/arc*(1-Math.cos(angle));}
            var p=pose.transformPosition(new Vector3f((float)x,i<2?-.5f:.5f,(float)z));
            sample[4+i*2]=p.x/(-p.z*ty*aspect);sample[5+i*2]=p.y/(-p.z*ty);
        }
        lastObserved=sample;
        if(enabled)samples.add(sample);
    }
}
