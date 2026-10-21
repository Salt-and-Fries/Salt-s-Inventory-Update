package com.salts_inventory_update.controlify.qa;

import com.salts_inventory_update.api.client.desktop.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.MouseButtonEvent;

/** A genuine client-only public-API view used exclusively by the opt-in QA mod. */
final class QaClientView implements DesktopWindowDefinition<QaClientView.Model, QaClientView.Model> {
    static final class Model {
        int statesCreated, opened, closed, clicks;
        int height = 100;
        int buttonX, buttonY;
        int externalX, externalY, externalClicks;
        boolean modalPopup;
        int popupDismissals;
        boolean navigationFixture, editing;
        int tab, shift, virtualClicks, scroll;
        String query = "";
    }

    @Override public Model createState(DesktopWindowSetupContext<Model> context) {
        context.model().statesCreated++;
        return context.model();
    }
    @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<Model> context) {
        return DesktopWindowSize.of(180, context.model().height);
    }
    @Override public DesktopWindowOptions options(DesktopWindowContext<Model, Model> context) {
        return new DesktopWindowOptions(false, false, true, false, false);
    }
    @Override public DesktopWindowSurface surface(DesktopWindowContext<Model, Model> context) {
        var regions = new java.util.ArrayList<DesktopWindowRegion>();
        regions.add(DesktopWindowRegion.visualAndInteractive(12, context.windowHeight() + 6, 145, 24).transientRegion());
        if (context.model().navigationFixture) {
            int x = context.contentX() - context.windowX() + context.model().shift;
            int y = context.contentY() - context.windowY();
            for (int i=0;i<3;i++) regions.add(DesktopWindowRegion.interactive(x+12+i*40,y,32,16).withRole(DesktopHitRole.CONTROL));
            regions.add(DesktopWindowRegion.interactive(x+12,y+70,132,16).withRole(DesktopHitRole.CONTROL));
            regions.add(DesktopWindowRegion.interactive(x+12,y+96,16,16).withRole(DesktopHitRole.SLOT));
        }
        return DesktopWindowSurface.SALT.withRegions(regions);
    }
    @Override public java.util.List<DesktopPopup> popups(DesktopWindowContext<Model, Model> context) {
        return context.model().modalPopup ? java.util.List.of(new DesktopPopup("qa-modal",
                java.util.List.of(DesktopWindowRegion.visualAndInteractive(12, 20, 145, 24)),
                true, true, false, false)) : java.util.List.of();
    }
    @Override public boolean dismissPopup(DesktopWindowContext<Model, Model> context, String id, DesktopPopup.Dismissal reason) {
        if (!id.equals("qa-modal") || !context.model().modalPopup || reason != DesktopPopup.Dismissal.OUTSIDE_PRESS) return false;
        context.model().modalPopup = false;
        context.model().popupDismissals++;
        return true;
    }
    @Override public void opened(DesktopWindowContext<Model, Model> context) {
        if (context.inventoryAvailable()) throw new IllegalStateException("Client-only view acquired an inventory");
        context.model().opened++;
    }
    @Override public void closed(DesktopWindowContext<Model, Model> context) { context.model().closed++; }
    @Override public void render(DesktopRenderContext<Model, Model> context) {
        Model model = context.model();
        model.buttonX = context.contentX() + 12;
        model.buttonY = context.contentY() + 24;
        context.fill(context.contentX(), context.contentY(), context.contentX() + context.contentWidth(),
                context.contentY() + context.contentHeight(), 0xFF203244);
        context.fill(model.buttonX, model.buttonY, model.buttonX + 145, model.buttonY + 24, 0xFF317F91);
        context.text("Press with controller", model.buttonX + 6, model.buttonY + 8, 0xFFFFFFFF, false);
        context.text("Clicks: " + model.clicks, model.buttonX, model.buttonY + 36, 0xFFFFFFFF, false);
        if (model.navigationFixture) {
            int x=context.contentX()+model.shift,y=context.contentY();
            for(int i=0;i<3;i++) {
                context.fill(x+12+i*40,y,x+44+i*40,y+16,model.tab==i?0xFF387C91:0xFF47515C);
                context.text("Tab "+i,x+14+i*40,y+4,0xFFFFFFFF,false);
            }
            context.fill(x+12,y+70,x+144,y+86,0xFF101820);
            context.text(model.query+"_",x+14,y+74,0xFFFFFFFF,false);
            context.fill(x+12,y+96,x+28,y+112,0xFF777733);
        }
    }
    @Override public void renderOverlay(DesktopRenderContext<Model, Model> context) {
        Model model = context.model();
        model.externalX = context.windowX() + 12;
        model.externalY = context.windowY() + context.windowHeight() + 6;
        context.fill(model.externalX, model.externalY, model.externalX + 145, model.externalY + 24, 0xFF78538B);
        context.text("Outside clicks: " + model.externalClicks, model.externalX + 6, model.externalY + 8, 0xFFFFFFFF, false);
        if (model.modalPopup) {
            context.fill(context.windowX() + 12, context.windowY() + 20,
                    context.windowX() + 157, context.windowY() + 44, 0xFF86444F);
            context.text("Modal popup", context.windowX() + 18, context.windowY() + 28, 0xFFFFFFFF, false);
        }
    }
    @Override public boolean mouseClicked(DesktopInputContext<Model, Model> context, MouseButtonEvent event, boolean doubleClick) {
        Model model = context.model();
        if (model.navigationFixture && event.button()==0) {
            double x=event.x()-context.contentX()-model.shift,y=event.y()-context.contentY();
            model.editing=x>=12 && x<144 && y>=70 && y<86;
            if (model.editing) return true;
            for(int i=0;i<3;i++) if(x>=12+i*40 && x<44+i*40 && y>=0 && y<16) {model.tab=i;return true;}
            if(x>=12 && x<28 && y>=96 && y<112) {model.virtualClicks++;return true;}
        }
        if (event.button() == 0 && event.x() >= model.buttonX && event.x() < model.buttonX + 145
                && event.y() >= model.buttonY && event.y() < model.buttonY + 24) {
            model.clicks++;
            return true;
        }
        if (event.button() == 0 && event.x() >= model.externalX && event.x() < model.externalX + 145
                && event.y() >= model.externalY && event.y() < model.externalY + 24) {
            model.externalClicks++;
            return true;
        }
        return false;
    }
    @Override public boolean mouseReleased(DesktopInputContext<Model, Model> context, MouseButtonEvent event) { return true; }
    @Override public java.util.List<DesktopWindowBounds> textInputBounds(DesktopWindowContext<Model,Model> context) {
        return context.model().navigationFixture?java.util.List.of(new DesktopWindowBounds(context.contentX()+12+context.model().shift,context.contentY()+70,132,16)):java.util.List.of();
    }
    @Override public boolean wantsTextInput(DesktopWindowContext<Model, Model> context) {
        return context.keyboardFocused() && context.model().editing;
    }
    @Override public java.util.List<DesktopScrollRegion> controllerScrollRegions(DesktopWindowContext<Model,Model> context) {
        if(!context.model().navigationFixture) return java.util.List.of();
        return java.util.List.of(new DesktopScrollRegion(context.contentX()+12+context.model().shift,context.contentY()+96,16,16,16,
            context.model().scroll>0,context.model().scroll<20));
    }
    @Override public boolean mouseScrolled(DesktopInputContext<Model,Model> context,double x,double y,double sx,double sy) {
        if(!context.model().navigationFixture) return false;
        var region=controllerScrollRegions(context).get(0);
        if(!region.contains(x,y)) return false;
        int before=context.model().scroll;
        context.model().scroll=Math.max(0,Math.min(20,before+(sy<0?1:-1)));
        return before!=context.model().scroll;
    }
    @Override public boolean charTyped(DesktopInputContext<Model, Model> context, net.minecraft.client.input.CharacterEvent event) {
        if(!wantsTextInput(context)) return false;
        context.model().query+=new String(Character.toChars(event.codepoint()));return true;
    }
    @Override public boolean keyPressed(DesktopInputContext<Model, Model> context, net.minecraft.client.input.KeyEvent event) {
        if(!wantsTextInput(context)) return false;
        if(event.key()==InputConstants.KEY_BACKSPACE && !context.model().query.isEmpty())
            context.model().query=context.model().query.substring(0,context.model().query.offsetByCodePoints(context.model().query.length(),-1));
        return true;
    }
}
