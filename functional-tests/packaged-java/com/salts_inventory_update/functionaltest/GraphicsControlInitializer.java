package com.salts_inventory_update.functionaltest;

/** No Salt dependencies. Isolates the game's graphics startup from both mods. */
public final class GraphicsControlInitializer implements net.fabricmc.api.ClientModInitializer {
    @Override public void onInitializeClient() {
        System.out.println("SIU_VANILLA_GRAPHICS_CONTROL_START");
        Thread timer=new Thread(()->{
            try { Thread.sleep(60000L); } catch(InterruptedException e) { return; }
            var client=net.minecraft.client.Minecraft.getInstance();
            client.execute(()->{System.out.println("SIU_VANILLA_GRAPHICS_CONTROL_FINISH");client.stop();});
        },"Disposable graphics control shutdown");
        timer.setDaemon(true);timer.start();
    }
}
