package com.salts_inventory_update.spatial;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Continuous view-space presentation, including retargets while the camera is still moving. */
public final class SpatialSurfaceTransition {
    public enum Mode { SPATIAL, MOVEMENT, CLOSING }
    private Matrix4f displayed, start;
    private Mode mode;
    private int revision = -1;
    private long lastTime;
    private float lastAnimation = 1, closeAnimation = 1;
    private final Vector3f positionVelocity=new Vector3f(), scaleVelocity=new Vector3f();

    public void reset() { displayed = start = null; mode = null; lastTime = 0; revision = -1; positionVelocity.zero();scaleVelocity.zero(); }

    public Matrix4f sample(Matrix4f spatial, Matrix4f normal, Mode next, int nextRevision,
                           double progress, float animation, long now, double durationMs) {
        if (displayed == null) displayed = new Matrix4f(normal).scale(animation);
        double elapsed = lastTime == 0 ? 0 : Math.max(0, (now - lastTime) / 1_000_000.0);
        lastTime = now;
        if (revision != nextRevision || mode != next) {
            start = new Matrix4f(displayed);
            closeAnimation = Math.max(.00001f, lastAnimation);
            mode = next;
            revision = nextRevision;
            positionVelocity.zero();scaleVelocity.zero();
        }
        if (next == Mode.CLOSING) {
            // Keep the exact last placement while upstream fades/shrinks its captured texture.
            displayed.set(start).scale(animation / closeAnimation);
        } else if (next == Mode.MOVEMENT) {
            displayed.set(interpolate(start, normal, (float) progress));
        } else {
            // The fit can change sides as the opening camera clears the avatar. Following it
            // continuously also covers Alt release, perspective changes and rapid reopen.
            displayed.set(follow(displayed, spatial, Math.min(elapsed,50)/1000, Math.max(150,durationMs)/1000));
        }
        lastAnimation = animation;
        return new Matrix4f(displayed);
    }

    private Matrix4f follow(Matrix4f from,Matrix4f to,double seconds,double duration) {
        var position=from.getTranslation(new Vector3f());var target=to.getTranslation(new Vector3f());
        float fromDepth=Math.max(.02f,-position.z),toDepth=Math.max(.02f,-target.z);
        float alpha=(float)(1-Math.exp(-seconds*4/duration));
        float depth=fromDepth+(toDepth-fromDepth)*alpha;
        var translation=damp(position.div(fromDepth),target.div(toDepth),positionVelocity,seconds,duration).mul(depth);
        var scale=damp(from.getScale(new Vector3f()).div(fromDepth),to.getScale(new Vector3f()).div(toDepth),scaleVelocity,seconds,duration).mul(depth);
        var rotation=from.getUnnormalizedRotation(new Quaternionf()).slerp(to.getUnnormalizedRotation(new Quaternionf()),alpha);
        return new Matrix4f().translationRotateScale(translation,rotation,scale);
    }

    /** Analytic critically damped motion: changing a fit target cannot instantly change velocity. */
    private static Vector3f damp(Vector3f value,Vector3f target,Vector3f velocity,double seconds,double duration) {
        float omega=(float)(4/duration),dt=(float)seconds,decay=(float)Math.exp(-omega*seconds);
        var offset=new Vector3f(value).sub(target);
        var temporary=new Vector3f(offset).mul(omega).add(velocity).mul(dt);
        velocity.sub(new Vector3f(temporary).mul(omega)).mul(decay);
        return offset.add(temporary).mul(decay).add(target);
    }

    private static Matrix4f interpolate(Matrix4f from, Matrix4f to, float amount) {
        var translation = from.getTranslation(new Vector3f());
        var destination = to.getTranslation(new Vector3f());
        float fromDepth=Math.max(.02f,-translation.z),toDepth=Math.max(.02f,-destination.z);
        float depth=fromDepth+(toDepth-fromDepth)*amount;
        // Interpolate projected placement and size. Interpolating world-space depth first
        // makes a far-away target pull the panel across most of the screen in one frame.
        translation.div(fromDepth).lerp(destination.div(toDepth),amount).mul(depth);
        var rotation = from.getUnnormalizedRotation(new Quaternionf()).slerp(to.getUnnormalizedRotation(new Quaternionf()), amount);
        var scale = from.getScale(new Vector3f()).div(fromDepth)
            .lerp(to.getScale(new Vector3f()).div(toDepth), amount).mul(depth);
        return new Matrix4f().translationRotateScale(translation, rotation, scale);
    }
}
