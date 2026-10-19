package com.salts_inventory_update.controlify.qa;

import com.salts_inventory_update.network.DesktopPackets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Deliberately ages only a selected outgoing state ID after the real mutation queue resolves
 * its packet. Native nonce, capabilities, sequence and item payloads remain untouched. */
public final class QaHoverPacketProbe {
    private static boolean staleClickArmed,staleBeginArmed,authenticQuickMoveArmed;
    private static int staleClicks,staleBegins;
    private static final Map<String,Object> evidence=new LinkedHashMap<>();
    private QaHoverPacketProbe() { }
    public static synchronized void staleNextClick() { staleClickArmed=true; }
    public static synchronized void staleNextBegin() { staleBeginArmed=true; }
    public static synchronized void observeNextQuickMove() { authenticQuickMoveArmed=true; }
    public static synchronized boolean staleClickSent() { return !staleClickArmed && staleClicks==1; }
    public static synchronized boolean staleBeginSent() { return !staleBeginArmed && staleBegins==1; }
    public static synchronized CustomPacketPayload outgoing(CustomPacketPayload payload) {
        if(payload instanceof DesktopPackets.DesktopRecipeHoverPayload hover && QaFeatureProbe.scenario.startsWith("contextual-hover-genuinely-stale-click")) {
            String cause=StackWalker.getInstance().walk(stream -> stream.limit(18).map(Object::toString).collect(java.util.stream.Collectors.joining(" <- ")));
            System.out.println("CONTROLIFY_QA_HOVER_PACKET action="+hover.action()+",epoch="+hover.epoch()+",state="+hover.stateId()+",cause="+cause);
        }
        if(staleClickArmed && payload instanceof DesktopPackets.DesktopClickPayload actual && actual.debugId()==8999) {
            staleClickArmed=false;staleClicks++;
            var sent=new DesktopPackets.DesktopClickPayload(actual.connectionNonce(),actual.mutationId(),actual.playerSessionToken(),
                actual.sessionId(),actual.sessionToken(),older(actual.stateId()),actual.debugId(),actual.slotIndex(),actual.button(),
                actual.inputName(),actual.clientCarried());
            evidence.put("staleClick",Map.of("actualFactoryState",actual.stateId(),"sentState",sent.stateId(),"session",sent.sessionId(),
                "mutation",sent.mutationId(),"preservedAuthAndItemPayload",actual.connectionNonce()==sent.connectionNonce()
                    && actual.playerSessionToken()==sent.playerSessionToken() && actual.sessionToken()==sent.sessionToken()
                    && actual.mutationId()==sent.mutationId() && actual.slotIndex()==sent.slotIndex() && actual.button()==sent.button()
                    && actual.inputName().equals(sent.inputName()) && net.minecraft.world.item.ItemStack.matches(actual.clientCarried(),sent.clientCarried())));
            return sent;
        }
        if(staleBeginArmed && payload instanceof DesktopPackets.DesktopRecipeHoverPayload actual && actual.action()==DesktopPackets.RECIPE_HOVER_BEGIN) {
            staleBeginArmed=false;staleBegins++;
            var sent=new DesktopPackets.DesktopRecipeHoverPayload(actual.connectionNonce(),actual.mutationId(),actual.playerSessionToken(),
                actual.sessionId(),actual.sessionToken(),older(actual.stateId()),actual.epoch(),actual.action(),actual.recipeIndex());
            evidence.put("staleBegin",Map.of("actualFactoryState",actual.stateId(),"sentState",sent.stateId(),"session",sent.sessionId(),
                "mutation",sent.mutationId(),"preservedAuthAndItemPayload",actual.connectionNonce()==sent.connectionNonce()
                    && actual.playerSessionToken()==sent.playerSessionToken() && actual.sessionToken()==sent.sessionToken()
                    && actual.mutationId()==sent.mutationId() && actual.epoch()==sent.epoch() && actual.action()==sent.action()
                    && actual.recipeIndex()==sent.recipeIndex()));
            return sent;
        }
        if(payload instanceof DesktopPackets.DesktopClickPayload actual && actual.debugId()==9001)
            evidence.put("authenticClick",Map.of("state",actual.stateId(),"session",actual.sessionId(),"mutation",actual.mutationId(),"untouched",true));
        if(authenticQuickMoveArmed && payload instanceof DesktopPackets.DesktopQuickMovePayload actual) {
            authenticQuickMoveArmed=false;
            evidence.put("authenticQuickMove",Map.of("state",actual.sourceStateId(),"session",actual.sourceSessionId(),"mutation",actual.mutationId(),"untouched",true));
        }
        return payload;
    }
    public static void serverHoverEnd(net.minecraft.server.level.ServerPlayer player,Object sessions) {
        if(!QaFeatureProbe.scenario.startsWith("contextual-hover-genuinely-stale-click")) return;
        String cause=StackWalker.getInstance().walk(stream -> stream.limit(18).map(Object::toString).collect(java.util.stream.Collectors.joining(" <- ")));
        System.out.println("CONTROLIFY_QA_SERVER_HOVER_END preview="+(read(sessions,"recipeHover")!=null)+",cause="+cause);
    }
    public static synchronized void serverInterruptionReturned(net.minecraft.server.level.ServerPlayer player,CustomPacketPayload payload) {
        if(!(payload instanceof DesktopPackets.DesktopClickPayload click) || click.debugId()!=8999) return;
        Object sessions=((Map<?,?>)read(com.salts_inventory_update.server.DesktopContainerSessions.class,"PLAYERS")).get(player);
        Object hover=read(sessions,"recipeHover");
        int nativeState=hover==null?-1:((net.minecraft.world.inventory.AbstractContainerMenu)read(hover,"menu")).getStateId();
        evidence.put("staleClickServerReturn",Map.of("previewRetained",hover!=null,"sentState",click.stateId(),
            "nativeState",nativeState,"carriedEmpty",player.inventoryMenu.getCarried().isEmpty()));
        System.out.println("CONTROLIFY_QA_SERVER_STALE_CLICK_RETURN preview="+(hover!=null)+",wireState="+click.stateId()
            +",nativeState="+nativeState
            +",nativeCarried="+player.inventoryMenu.getCarried());
    }
    private static Object read(Object target,String name) {
        try {var field=(target instanceof Class<?> type?type:target.getClass()).getDeclaredField(name);field.setAccessible(true);return field.get(target instanceof Class<?>?null:target);}
        catch(ReflectiveOperationException error) {throw new IllegalStateException(error);}
    }
    private static int older(int state) { return (state+32760)&32767; }
    public static synchronized void leaseObserved(long startTick,long touchedTick,long endTick) {
        evidence.put("nativeLease",Map.of("startTick",startTick,"touchedTick",touchedTick,"endTick",endTick,
            "nativeTicksSinceTouch",endTick-touchedTick));
    }
    public static synchronized void cookingEntryObserved(int ironBefore,int ironAfter,int bedrockBefore,int bedrockAfter,
        int inputCount,int outputCount,int fuelCount,String ironLocations,String bedrockLocations) {
        evidence.put("nativeCookingEntry",Map.of("ironBefore",ironBefore,"ironAfter",ironAfter,"bedrockBefore",bedrockBefore,
            "bedrockAfter",bedrockAfter,"inputCount",inputCount,"outputCount",outputCount,"fuelCount",fuelCount,
            "ironLocations",ironLocations,"bedrockLocations",bedrockLocations));
    }
    public static synchronized Map<String,Object> snapshot() {
        return Map.of("contextualHoverPacketEvidence",Map.copyOf(evidence),"contextualStaleClickPackets",staleClicks,
            "contextualStaleBeginPackets",staleBegins,"contextualStalePacketArmsConsumed",!staleClickArmed && !staleBeginArmed && !authenticQuickMoveArmed);
    }
}
