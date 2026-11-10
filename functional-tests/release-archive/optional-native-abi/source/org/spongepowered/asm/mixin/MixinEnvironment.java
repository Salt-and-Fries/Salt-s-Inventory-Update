package org.spongepowered.asm.mixin;
/** Explicit unit-test environment; never loaded into Minecraft. */
public final class MixinEnvironment {
    public enum Side { UNKNOWN, CLIENT, SERVER }
    private static final MixinEnvironment VALUE=new MixinEnvironment();
    public static MixinEnvironment getCurrentEnvironment(){return VALUE;}
    public Side getSide(){return probe.OptionalAbiMain.server?Side.SERVER:Side.CLIENT;}
}
