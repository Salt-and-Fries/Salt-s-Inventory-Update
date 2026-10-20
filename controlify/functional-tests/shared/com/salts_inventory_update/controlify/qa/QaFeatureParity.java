package com.salts_inventory_update.controlify.qa;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.server.DesktopContainerSessions;
import com.salts_inventory_update.controlify.WorkspaceSearchKeyboard;
import com.salts_inventory_update.controlify.model.ContextualWheelSelection;
import com.salts_inventory_update.controlify.model.RecipeFilter;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The same added-feature assertions run through each legacy target's genuine native driver. */
public final class QaFeatureParity {
    public interface Harness {
        void step(String name, Runnable start, BooleanSupplier ready, Runnable verify, int settle);
        void button(String name, String nativeButton, Runnable verify);
        void check(String name, boolean passed);
        void capture(String name);
        QaWorkspaceOwner owner();
        Screen screen();
        void neutral();
        void leftTrigger(float value);
        void rightTrigger(float value);
        void rightStick(float x, float y);
        void holdButton(String nativeButton, boolean down);
        void aimWindow(UUID window);
        void point(UUID window, double nativeX, double nativeY);
        void openContainer(String name, String which);
        UUID window(String which);
        void server(Runnable operation);
        boolean serverDone();
        ServerPlayer player();
        Container storage();
        AbstractContainerMenu menu(Class<? extends AbstractContainerMenu> kind);
        void awardRecipes();
        DesktopWindowHandle openView(QaClientView.Model model);
        boolean creative();
    }

    private final Harness h;
    private final QaClientView.Model view = new QaClientView.Model();
    private DesktopWindowHandle viewHandle;
    private long heldAt;
    private double positionBefore;
    private Object wheel;
    private Item creativeItem;
    private QaFeatureParity(Harness harness) { h = harness; }
    public static void append(Harness harness) {
        var fixture = new QaFeatureParity(harness);
        if ("playtest-adapters".equals(System.getProperty("salts_inventory_controlify.qaScope"))) fixture.playtestAdapters();
        else fixture.scenarios();
    }

    /** Only the playtest changes; deliberately excludes the broader feature-parity scenarios. */
    private void playtestAdapters() {
        step("playtest-hold-default", () -> {
            try { h.owner().config().setToggleWindowWheel(false); h.owner().config().setToggleRecipeWheel(false); }
            catch (Exception failure) { throw new IllegalStateException(failure); }
        }, () -> true, () -> {}, 2);
        controlsSettings();
        h.openContainer("parity-playtest-open-chest", "chest");
        h.openContainer("parity-playtest-open-table", "table");
        pair("table", "inventory");
        server("playtest-recipe-materials", () -> {
            h.player().getInventory().clearContent();
            h.storage().clearContent();
            h.player().getInventory().setItem(9, new ItemStack(Items.OAK_LOG, 1));
            h.player().getInventory().setItem(10, new ItemStack(Items.OAK_PLANKS, 12));
            h.awardRecipes(); h.player().inventoryMenu.broadcastChanges();
        }, () -> {
            try {
                var preferences = (com.salts_inventory_update.controlify.model.RecipeFilterSettings)
                    field(field(h.owner(), "value"), "recipeFilters");
                preferences.set(RecipeFilter.CRAFTABLE);
                h.owner().config().setToggleWindowWheel(true); h.owner().config().setToggleRecipeWheel(true);
            } catch (Exception failure) { throw new IllegalStateException(failure); }
        });
        step("playtest-LT-toggle-on", () -> h.leftTrigger(1), () -> h.owner().radialActive(), () -> {}, 3);
        step("playtest-LT-toggle-release", h::neutral, () -> true,
            () -> check("playtest-LT-stays-open-after-release", h.owner().radialActive()), 6);
        step("playtest-LT-toggle-off", () -> h.leftTrigger(1), this::side, () -> {}, 4);
        step("playtest-LT-neutral", h::neutral, this::side, () -> {}, 4);
        step("playtest-RT-toggle-on", () -> { pointBook("table"); h.rightTrigger(1); }, this::contextualActive,
            () -> wheel = field(h.screen(), "contextual"), 6);
        step("playtest-RT-toggle-release", h::neutral, () -> true,
            () -> check("playtest-RT-stays-open-after-release", contextualActive()), 6);
        step("playtest-aim-planks", () -> aimRecipe(Items.OAK_PLANKS), () -> selected(Items.OAK_PLANKS), h::neutral, 8);
        List<String> order = new ArrayList<>();
        final Object[] selectedId = {null};
        step("playtest-remember-carousel", () -> {}, () -> true, () -> {
            order.addAll(entries().stream().map(r -> r.id().toString()).toList());
            positionBefore = selection().position(); selectedId[0] = entries().get(selection().displayedSelected(entries().size())).id();
        }, 4);
        button("playtest-craft-A", "SOUTH_BUTTON", () -> {});
        serverAssert("playtest-crafts-four-planks", () -> h.player().getInventory().getItem(0).is(Items.OAK_PLANKS)
            && h.player().getInventory().getItem(0).getCount() == 4);
        step("playtest-carousel-after-crafting", h::neutral, () -> true, () -> {
            check("playtest-catalog-does-not-move", order.equals(entries().stream().map(r -> r.id().toString()).toList()));
            check("playtest-wheel-angle-does-not-move", Math.abs(selection().position() - positionBefore) < .0001);
            var recipe = entries().get(selection().displayedSelected(entries().size()));
            check("playtest-selected-recipe-does-not-move", selectedId[0].equals(recipe.id()));
            check("playtest-recipe-availability-refreshes", !recipe.craftable());
            h.capture("playtest-adapter-stable-crafting");
        }, 10);
        step("playtest-RT-toggle-off", () -> h.rightTrigger(1), () -> !contextualActive(), () -> {}, 5);
        step("playtest-restore-hold-mode", () -> {
            h.neutral();
            try { h.owner().config().setToggleWindowWheel(false); h.owner().config().setToggleRecipeWheel(false); }
            catch (Exception failure) { throw new IllegalStateException(failure); }
        }, this::side, () -> {}, 6);
        server("playtest-pickup-fixture", () -> {
            h.player().getInventory().clearContent(); h.player().inventoryMenu.setCarried(ItemStack.EMPTY);
            h.player().getInventory().setItem(9, new ItemStack(Items.DIAMOND, 7));
            h.player().inventoryMenu.slots.get(1).set(new ItemStack(Items.EMERALD, 3));
            h.player().inventoryMenu.broadcastChanges();
        }, () -> {});
        step("playtest-pickup-point", () -> pointItem(Items.DIAMOND), () -> true, () -> {}, 5);
        button("playtest-pickup-A", "SOUTH_BUTTON", () -> {});
        serverAssert("playtest-native-cursor-holds-seven", () -> h.player().inventoryMenu.getCarried().is(Items.DIAMOND)
            && h.player().inventoryMenu.getCarried().getCount() == 7);
        step("playtest-cursor-preview", h::neutral, () -> !workspace().carriedStack().isEmpty(),
            () -> h.capture("playtest-adapter-carried-item"), 6);
        closeWorkspace("playtest-close-cursor");
        serverAssert("playtest-close-settles-cursor-and-grid", () -> h.player().inventoryMenu.getCarried().isEmpty()
            && h.player().inventoryMenu.slots.subList(1, 5).stream().noneMatch(s -> s.hasItem())
            && inventoryCount(Items.DIAMOND) == 7 && inventoryCount(Items.EMERALD) == 3);
        h.openContainer("parity-playtest-open-furnace", "furnace");
        server("playtest-fuel-fixture", () -> {
            h.player().getInventory().setItem(9, new ItemStack(Items.COAL, 11));
            h.player().inventoryMenu.broadcastChanges();
        }, () -> {});
        step("playtest-fuel-point", () -> pointItem(Items.COAL), () -> true, () -> {}, 5);
        button("playtest-fuel-quick-move", "MISC_1_BUTTON", () -> {});
        serverAssert("playtest-fuel-reaches-fuel-slot", () -> h.menu(FurnaceMenu.class).slots.get(0).getItem().isEmpty()
            && h.menu(FurnaceMenu.class).slots.get(1).getItem().is(Items.COAL)
            && h.menu(FurnaceMenu.class).slots.get(1).getItem().getCount() == 11);
        server("playtest-full-fuel-fixture", () -> {
            h.menu(FurnaceMenu.class).slots.get(1).set(new ItemStack(Items.COAL, 64));
            h.player().getInventory().setItem(9, new ItemStack(Items.COAL, 7));
            h.menu(FurnaceMenu.class).broadcastChanges(); h.player().inventoryMenu.broadcastChanges();
        }, () -> {});
        step("playtest-full-fuel-point", () -> pointItem(Items.COAL), () -> true, () -> {}, 5);
        button("playtest-full-fuel-quick-move", "MISC_1_BUTTON", () -> {});
        serverAssert("playtest-full-fuel-preserves-source", () -> h.menu(FurnaceMenu.class).slots.get(0).getItem().isEmpty()
            && h.menu(FurnaceMenu.class).slots.get(1).getItem().getCount() == 64
            && h.player().getInventory().getItem(9).is(Items.COAL) && h.player().getInventory().getItem(9).getCount() == 7);
        final UUID[] previousFocus = {null};
        step("playtest-swap-aim", () -> { previousFocus[0] = h.owner().model().focused(); h.leftTrigger(1); h.aimWindow(h.window("furnace")); },
            () -> h.owner().radialActive(), () -> {}, 5);
        button("playtest-swap-A", "SOUTH_BUTTON", () -> {});
        step("playtest-swap-stick-held", () -> {}, () -> true, () -> {
            check("playtest-swap-keeps-held-direction", h.owner().model().focused().equals(h.window("furnace"))
                && h.owner().model().highlighted().filter(previousFocus[0]::equals).isPresent());
            h.capture("playtest-adapter-swap");
        }, 6);
        step("playtest-swap-release", h::neutral, this::side, () -> {}, 6);
        closeWorkspace("playtest-finish");
    }

    private void pointBook(String name) {
        var bounds = workspace().contextualBookHint(h.window(name)).activationBounds();
        h.point(h.window(name), bounds.x() + bounds.width() / 2.0, bounds.y() + bounds.height() / 2.0);
    }

    private void pointItem(Item item) {
        for (var hit : workspace().visibleSlots(inventory())) {
            var slot = workspace().hoveredSlot(inventory(), hit.x() + 8, hit.y() + 8);
            if (slot != null && slot.getItem().is(item)) { h.point(inventory(), hit.x() + 8, hit.y() + 8); return; }
        }
        throw new IllegalStateException("Missing native inventory item: " + item);
    }

    private void controlsSettings() {
        step("playtest-native-controls-open", () -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            try {
                Class<?> type;
                try { type = Class.forName("net.minecraft.client.gui.screens.options.controls.ControlsScreen"); }
                catch (ClassNotFoundException legacy) { type = Class.forName("net.minecraft.client.gui.screens.controls.ControlsScreen"); }
                mc.setScreen((Screen) type.getConstructor(Screen.class, net.minecraft.client.Options.class).newInstance(mc.screen, mc.options));
            } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
        }, () -> true, () -> {
            var entry = h.screen().children().stream().filter(c -> c instanceof net.minecraft.client.gui.components.Button button
                && button.getMessage().equals(net.minecraft.network.chat.Component.translatable("salts_inventory_controlify.controls.entry")))
                .map(c -> (net.minecraft.client.gui.components.Button) c).findFirst().orElseThrow();
            check("playtest-controls-entry-visible", entry.visible && entry.active);
            pressNativeButton(entry);
        }, 5);
        step("playtest-native-controls-settings", () -> {},
            () -> h.screen().getClass().getSimpleName().equals("WorkspaceControlsScreen"),
            () -> { check("playtest-native-settings-open", true); h.capture("playtest-adapter-settings"); h.screen().onClose(); }, 5);
        step("playtest-leave-native-controls", () -> h.screen().onClose(), () -> h.screen() == null, () -> {}, 5);
    }

    private static void pressNativeButton(net.minecraft.client.gui.components.Button button) {
        try {
            for (Method method : button.getClass().getMethods()) if (method.getName().equals("onPress")) {
                if (method.getParameterCount() == 0) { method.invoke(button); return; }
                if (method.getParameterCount() == 1) {
                    Class<?> event = Class.forName("net.minecraft.client.input.KeyEvent");
                    Object input = event.getConstructor(int.class, int.class, int.class).newInstance(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0);
                    method.invoke(button, input); return;
                }
            }
            throw new NoSuchMethodException("Button.onPress");
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }
    private void step(String name, Runnable start, BooleanSupplier ready, Runnable verify, int settle) {
        h.step("parity-" + name, start, ready, verify, settle);
    }
    private void check(String name, boolean value) { h.check("parity-" + name, value); }
    private void button(String name, String key, Runnable verify) { h.button("parity-" + name, key, verify); }
    private DesktopWorkspace workspace() { return h.owner().desktop(); }
    private UUID inventory() { return h.owner().model().playerInventory(); }
    private boolean side() { return h.owner().workspaceActive() && !h.owner().radialActive() && !h.owner().tuckedAway(); }
    private void server(String name, Runnable action, Runnable verify) {
        step(name, () -> h.server(action), h::serverDone, verify, 6);
    }
    private void serverAssert(String name, BooleanSupplier condition) {
        final boolean[] result = {false};
        step(name, () -> h.server(() -> result[0] = condition.getAsBoolean()),
            () -> {
                if(!h.serverDone()) return false;
                if(result[0]) return true;
                h.server(() -> result[0]=condition.getAsBoolean());return false;
            }, () -> check(name, result[0]), 10);
    }
    private void pair(String first, String second) {
        step("focus-" + first, () -> { h.neutral(); h.leftTrigger(1); h.aimWindow(h.window(first)); },
            () -> h.owner().radialActive(), () -> {}, 4);
        button("focus-" + first + "-A", "SOUTH_BUTTON", () -> check("source-focused", h.owner().model().focused().equals(h.window(first))));
        step("leave-focus-wheel", h::neutral, () -> !h.owner().radialActive(), () -> {}, 4);
        step("pair-" + second, () -> { h.leftTrigger(1); h.aimWindow(h.window(second)); },
            () -> h.owner().radialActive(), () -> {}, 4);
        step("commit-pair", h::neutral, this::side,
            () -> check("destination-selected", h.owner().model().selected().equals(h.window(second))), 8);
    }
    private void scenarios() {
        closeWorkspace("start-clean");
        button("start-open-inventory","NORTH_BUTTON", () -> check("start-native-workspace",side()));
        apiEditor();
        crafting();
        cooking();
        creative();
    }
    private void closeWorkspace(String name) {
        step(name+"-press-B", () -> {h.neutral();h.holdButton("EAST_BUTTON",true);},
            () -> !h.owner().workspaceActive(), () -> {}, 3);
        step(name+"-neutral",h::neutral,() -> h.screen()==null,() -> {},12);
    }
    private void apiEditor() {
        step("api-view-open", () -> {
            h.neutral(); view.height=190; view.navigationFixture=true;
            viewHandle=h.openView(view);
        }, () -> viewHandle != null && h.owner().windows().containsKey(viewHandle.instanceId()) && view.buttonX != 0,
            () -> check("api-controls-discovered", workspace().controllerTargets(viewHandle.instanceId()).size() >= 5), 10);
        step("api-geometry-virtual-authority", () -> {}, () -> true, () -> {
            check("api-no-real-slots", workspace().visibleSlots(viewHandle.instanceId()).isEmpty());
            check("api-no-text-focus-before-click", !workspace().textInputActive(viewHandle.instanceId()));
            check("api-scroll-viewport", workspace().controllerScrollAvailable(viewHandle.instanceId(),view.buttonX+8,view.buttonY+80));
            h.capture("parity-api-navigation");
            pointView(18,8);
        }, 6);
        button("api-Dpad-tab", "DPAD_RIGHT_BUTTON", () -> {});
        button("api-tab-select", "SOUTH_BUTTON", () -> check("api-native-tab-selected", view.tab > 0));
        step("api-virtual-entry-point", () -> pointView(18,104), () -> true, () -> {}, 4);
        button("api-virtual-entry-A", "SOUTH_BUTTON", () -> check("api-virtual-native-click", view.virtualClicks == 1));
        step("api-scroll-edge-point", () -> pointView(18,104), () -> true, () -> {}, 3);
        button("api-one-row-Dpad", "DPAD_DOWN_BUTTON", () -> check("api-scroll-one-native-row", view.scroll == 1));
        step("api-right-stick-scroll", () -> { pointView(18,104); h.rightStick(0,1); heldAt=System.nanoTime(); },
            () -> System.nanoTime()-heldAt > 250_000_000L,
            () -> { h.neutral(); check("api-right-stick-native-scroll",view.scroll > 1); }, 2);
        step("api-editor-point", () -> pointView(40,78), () -> true, () -> {}, 4);
        button("api-editor-A", "SOUTH_BUTTON", () -> check("api-editor-keyboard-open", WorkspaceSearchKeyboard.active()));
        step("api-keyboard-neutral", h::neutral,
            () -> WorkspaceSearchKeyboard.active() && !Boolean.TRUE.equals(field(h.screen(),"openingAwaitingNeutral")), () -> {
                check("api-editor-own-text-focus", workspace().textInputActive(viewHandle.instanceId()));
                type(h.screen(),'a'); type(h.screen(),'b');
                check("api-editor-native-typing", view.query.equals("ab"));
                h.capture("parity-modded-keyboard");
            }, 8);
        step("api-keyboard-exclusive", () -> {
            h.leftTrigger(1); h.rightTrigger(1); h.rightStick(1,0);
            // North is the native keyboard's Enter shortcut; exercise item actions without submitting it.
            h.holdButton("MISC_1_BUTTON",true); h.holdButton("RIGHT_SHOULDER_BUTTON",true);
        }, () -> true, () -> {
            check("api-keyboard-keeps-workspace", WorkspaceSearchKeyboard.active() && viewHandle.isOpen()
                && !h.owner().tuckedAway() && !h.owner().radialActive());
            check("api-keyboard-captures-item-actions", view.virtualClicks == 1);
            h.neutral();
        }, 6);
        button("api-keyboard-close-B", "EAST_BUTTON", () -> {});
        step("api-keyboard-return", h::neutral, () -> side() && !WorkspaceSearchKeyboard.active() && !WorkspaceSearchKeyboard.waitingNeutral(),
            () -> check("api-keyboard-restores-input", viewHandle.isOpen() && view.query.equals("ab")), 8);
        step("api-retained-focus-item-point", () -> pointView(18,104), () -> true, () -> {}, 4);
        button("api-retained-focus-item-A", "SOUTH_BUTTON", () -> {
            check("api-retained-focus-does-not-reopen-keyboard", !WorkspaceSearchKeyboard.active());
            check("api-item-controls-restored", view.virtualClicks == 2);
        });
        step("api-view-close", () -> { viewHandle.close(); h.neutral(); }, () -> !viewHandle.isOpen(), () -> {}, 8);
    }
    private void pointView(int x, int y) { h.point(viewHandle.instanceId(),view.buttonX-12+x,view.buttonY-24+y); }
    private void crafting() {
        h.openContainer("parity-open-resource-chest","chest");
        h.openContainer("parity-open-crafting-table","table");
        server("seed-unlocked-crafting", () -> {
            h.player().getInventory().clearContent(); h.player().inventoryMenu.setCarried(ItemStack.EMPTY);
            h.storage().clearContent(); h.storage().setItem(0,new ItemStack(Items.OAK_LOG,20));
            var menu=h.menu(CraftingMenu.class);
            for(int i=1;i<=9;i++) menu.slots.get(i).set(ItemStack.EMPTY);
            menu.slots.get(1).set(new ItemStack(Items.STICK,3));
            h.awardRecipes(); h.player().inventoryMenu.broadcastChanges(); menu.broadcastChanges();
        }, () -> {});
        step("filter-reset-fixture", () -> {
            Object client=field(h.owner(),"value");
            var settings=(com.salts_inventory_update.controlify.model.RecipeFilterSettings)field(client,"recipeFilters");
            try {settings.set(RecipeFilter.ALL);} catch(java.io.IOException failure) {throw new IllegalStateException(failure);}
        }, () -> true, () -> {}, 2);
        pair("table","inventory");
        step("wheel-open-RT", () -> {
            var bounds=workspace().contextualBookHint(h.window("table")).activationBounds();
            h.point(h.window("table"),bounds.x()+bounds.width()/2.0,bounds.y()+bounds.height()/2.0);
            h.rightTrigger(1);
        }, () -> contextualActive(), () -> {
            wheel=field(h.screen(),"contextual");
            check("wheel-native-crafting-context", workspace().contextualMenu(h.window("table")).kind()==DesktopContextualMenu.Kind.CRAFTING);
            check("wheel-counts-other-container-materials", entries().stream().anyMatch(r -> r.output().is(Items.OAK_PLANKS) && r.craftable()));
            h.capture("parity-crafting-wheel");
        }, 12);
        serverAssert("wheel-clears-existing-grid", () -> allOpenOwnedCount(Items.STICK)==3 && h.menu(CraftingMenu.class).slots.subList(1,10).stream().noneMatch(s -> s.getItem().is(Items.STICK)));
        step("wheel-aim-oak-planks", () -> aimRecipe(Items.OAK_PLANKS), () -> selected(Items.OAK_PLANKS),
            () -> check("wheel-right-stick-selects-recipe", selected(Items.OAK_PLANKS)), 8);
        button("wheel-craft-A","SOUTH_BUTTON", () -> {});
        serverAssert("wheel-crafts-real-native-result", () -> inventoryCount(Items.OAK_PLANKS)==4);
        serverAssert("wheel-output-hotbar-first", () -> h.player().getInventory().getItem(0).is(Items.OAK_PLANKS));
        button("wheel-craft-stack-Y","MISC_1_BUTTON", () -> {});
        serverAssert("wheel-stack-obeys-64-item-limit", () -> inventoryCount(Items.OAK_PLANKS)==68);
        step("wheel-deadzone-settle", () -> h.rightStick(0,0), () -> true,
            () -> positionBefore=selection().position(), 6);
        step("wheel-large-deadzone", () -> h.rightStick(.3f,.3f), () -> true,
            () -> { check("wheel-small-stick-does-not-shift", Math.abs(selection().position()-positionBefore)<.0001);h.rightStick(0,0); }, 6);
        step("wheel-bumper-held", () -> { h.holdButton("RIGHT_SHOULDER_BUTTON",true);positionBefore=selection().position();heldAt=System.nanoTime(); },
            () -> System.nanoTime()-heldAt > 300_000_000L,
            () -> { h.holdButton("RIGHT_SHOULDER_BUTTON",false);check("wheel-bumper-continuous-rotation",Math.abs(selection().position()-positionBefore)>.25); }, 2);
        button("wheel-filter-parts","DPAD_UP_BUTTON", () -> check("wheel-parts-filter", field(wheel,"filter")==RecipeFilter.CONTAINS_PARTS));
        button("wheel-filter-craftable","DPAD_UP_BUTTON", () -> check("wheel-craftable-filter", field(wheel,"filter")==RecipeFilter.CRAFTABLE));
        step("wheel-close-RT", h::neutral, () -> !contextualActive(), () -> {}, 10);
        serverAssert("wheel-no-items-lost", () -> allOpenOwnedCount(Items.STICK)==3 && totalLogs()==3 && inventoryCount(Items.OAK_PLANKS)==68 && allOpenOwnedCount(Items.OAK_PLANKS)==68);
        step("wheel-filter-reopen", () -> {
            var b=workspace().contextualBookHint(h.window("table")).activationBounds();h.point(h.window("table"),b.x()+b.width()/2.0,b.y()+b.height()/2.0);h.rightTrigger(1);
        }, this::contextualActive, () -> {
            wheel=field(h.screen(),"contextual");check("wheel-filter-retained",field(wheel,"filter")==RecipeFilter.CRAFTABLE);
        }, 10);
        step("wheel-finish",h::neutral,() -> !contextualActive(),() -> {},8);
    }
    private void cooking() {
        h.openContainer("parity-open-furnace","furnace");
        server("seed-cooking-resources", () -> {
            var menu=h.menu(FurnaceMenu.class);
            menu.slots.get(0).set(ItemStack.EMPTY);menu.slots.get(1).set(ItemStack.EMPTY);menu.slots.get(2).set(ItemStack.EMPTY);
            h.storage().setItem(1,new ItemStack(Items.IRON_ORE,3));h.storage().setItem(2,new ItemStack(Items.COAL,2));
            h.awardRecipes();menu.broadcastChanges();h.player().inventoryMenu.broadcastChanges();
        }, () -> {});
        pair("furnace","inventory");
        step("cooking-wheel-open", () -> {
            var b=workspace().contextualBookHint(h.window("furnace")).activationBounds();h.point(h.window("furnace"),b.x()+b.width()/2.0,b.y()+b.height()/2.0);h.rightTrigger(1);
        },this::contextualActive,() -> {
            wheel=field(h.screen(),"contextual");
            check("cooking-native-context",workspace().contextualMenu(h.window("furnace")).kind()==DesktopContextualMenu.Kind.COOKING);
            check("cooking-other-container-supply",entries().stream().anyMatch(r -> r.output().is(Items.IRON_INGOT) && r.craftable()));
        },10);
        step("cooking-aim-iron",() -> aimRecipe(Items.IRON_INGOT),() -> selected(Items.IRON_INGOT),() -> {},6);
        button("cooking-prepare-A","SOUTH_BUTTON",() -> {});
        serverAssert("cooking-native-input-and-fuel",() -> h.menu(FurnaceMenu.class).slots.get(0).getItem().is(Items.IRON_ORE)
            && (h.menu(FurnaceMenu.class).slots.get(1).hasItem() || ((FurnaceMenu)h.menu(FurnaceMenu.class)).isLit()));
        step("cooking-wheel-release",h::neutral,() -> !contextualActive(),() -> {h.capture("parity-cooking-native-window");},8);
    }
    private void creative() {
        closeWorkspace("before-creative");
        server("creative-game-mode",() -> {
            h.player().setGameMode(GameType.CREATIVE);h.player().getInventory().clearContent();h.player().inventoryMenu.setCarried(ItemStack.EMPTY);
            h.player().inventoryMenu.sendAllDataToRemote();
        },() -> {});
        step("creative-client-mode",() -> {},h::creative,() -> {},8);
        button("creative-open-Y","NORTH_BUTTON",() -> check("creative-native-workspace",side()));
        step("creative-select-building-tab",() -> {
            var content=workspace().contextualMenu(inventory());
            check("creative-native-tab-snapshot",content.kind()==DesktopContextualMenu.Kind.CREATIVE && content.tabs().size()>1);
            var tab=content.tabs().stream().filter(t -> t.id().toString().endsWith(":building_blocks")).findFirst().orElseThrow();
            check("creative-api-selects-tab",workspace().selectContextualTab(inventory(),tab.id()));
        },() -> !workspace().controllerTargets(inventory()).isEmpty(),() -> {},6);
        step("creative-catalog-point",this::pointCatalog,() -> true,() -> {
            check("creative-tabs-and-catalog-navigation",workspace().controllerTargets(inventory()).stream().anyMatch(t -> t.group().equals("creative-top-tabs")));
        },5);
        button("creative-catalog-A","SOUTH_BUTTON",() -> {});
        server("creative-native-item-check",() -> {
            var stack=h.player().getInventory().getItem(0);creativeItem=stack.getItem();
            check("creative-A-one-to-hotbar",!stack.isEmpty() && stack.getCount()==1 && h.player().inventoryMenu.getCarried().isEmpty());
        },() -> {});
        step("creative-pickup-hotbar",() -> pointHotbar(),() -> true,() -> {},4);
        button("creative-pickup-A","SOUTH_BUTTON",() -> {});
        serverAssert("creative-real-cursor-before-catalog",() -> h.player().inventoryMenu.getCarried().is(creativeItem));
        step("creative-clear-catalog-point",this::pointCatalog,() -> true,() -> {},4);
        button("creative-clear-carried-A","SOUTH_BUTTON",() -> {});
        serverAssert("creative-catalog-clears-without-grant",() -> h.player().inventoryMenu.getCarried().isEmpty() && inventoryCount(creativeItem)==0);
        step("creative-refill-catalog-point",this::pointCatalog,() -> true,() -> {},4);
        button("creative-refill-A","SOUTH_BUTTON",() -> {});
        step("creative-delete-hotbar-point",this::pointHotbar,() -> true,() -> {},4);
        button("creative-delete-quick-move","MISC_1_BUTTON",() -> {});
        serverAssert("creative-Y-deletes-only-hotbar",() -> inventoryCount(creativeItem)==0);
        step("creative-search-tab",() -> {
            var tab=workspace().contextualMenu(inventory()).tabs().stream().filter(t -> t.id().toString().endsWith(":search")).findFirst().orElseThrow();
            check("creative-search-selects-native-tab",workspace().selectContextualTab(inventory(),tab.id()));
        },() -> true,() -> {},6);
        step("creative-search-field-point",() -> {
            var b=workspace().visibleControls(inventory()).stream().filter(r -> r.height()==12 && r.width()>=24).findFirst().orElseThrow();
            h.point(inventory(),b.x()+b.width()/2.0,b.y()+b.height()/2.0);
        },() -> true,() -> {},4);
        button("creative-search-X","WEST_BUTTON",() -> check("creative-shortcut-opens-keyboard",WorkspaceSearchKeyboard.active()));
        step("creative-keyboard-neutral",h::neutral,() -> WorkspaceSearchKeyboard.active() && !Boolean.TRUE.equals(field(h.screen(),"openingAwaitingNeutral")),() -> {
            type(h.screen(),'i');type(h.screen(),'r');type(h.screen(),'o');type(h.screen(),'n');h.capture("parity-creative-search-keyboard");
        },6);
        button("creative-keyboard-close","EAST_BUTTON",() -> {});
        step("creative-keyboard-return",h::neutral,() -> side() && !WorkspaceSearchKeyboard.active() && !WorkspaceSearchKeyboard.waitingNeutral(),
            () -> check("creative-search-restores-controls",side()),8);
    }
    private void pointCatalog() {
        var t=workspace().controllerTargets(inventory()).stream().filter(v -> v.group().equals("creative-catalog")).findFirst().orElseThrow();
        h.point(inventory(),t.x()+t.width()/2.0,t.y()+t.height()/2.0);
    }
    private void pointHotbar() {
        for(var hit:workspace().visibleSlots(inventory())) {
            var slot=workspace().hoveredSlot(inventory(),hit.x()+8,hit.y()+8);
            if(slot!=null && slot.index==36) {h.point(inventory(),hit.x()+8,hit.y()+8);return;}
        }
        throw new IllegalStateException("Native creative hotbar slot missing");
    }
    private int inventoryCount(Item item) {
        int count=0;Container inventory=h.player().getInventory();
        for(int i=0;i<inventory.getContainerSize();i++) if(inventory.getItem(i).is(item)) count+=inventory.getItem(i).getCount();
        return count;
    }
    /** Count actual owned stacks once across native menu aliases, never derived crafting outputs. */
    private int allOpenOwnedCount(Item item) {
        List<AbstractContainerMenu> menus=new ArrayList<>();menus.add(h.player().inventoryMenu);
        Map<?,?> registry=(Map<?,?>)field(DesktopContainerSessions.class,"PLAYERS");
        Object sessions=registry.get(h.player());
        if(sessions==null)sessions=registry.get(h.player().connection);
        if(sessions==null)throw new IllegalStateException("Missing authoritative open-menu sessions for conservation check");
        for(Object session:((Map<?,?>)field(sessions,"sessions")).values())
            menus.add((AbstractContainerMenu)field(session,"menu"));
        IdentityHashMap<Object,Set<Integer>> seen=new IdentityHashMap<>();
        int count=0;
        for(AbstractContainerMenu menu:menus)for(var slot:DesktopMenuBindings.all(menu)) {
            if(slot instanceof net.minecraft.world.inventory.ResultSlot || DesktopMenuBindings.isVirtualSlot(menu,slot))continue;
            Object owner=DesktopMenuBindings.physicalOwner(menu,slot);
            int index=DesktopMenuBindings.physicalIndex(menu,slot);
            if(!seen.computeIfAbsent(owner,ignored -> new HashSet<>()).add(index))continue;
            ItemStack stack=slot.getItem();if(stack.is(item))count+=stack.getCount();
        }
        return count;
    }
    private int totalLogs() {return allOpenOwnedCount(Items.OAK_LOG);}
    private boolean contextualActive() { return Boolean.TRUE.equals(invoke(h.screen(),"contextualRadialActive")); }
    @SuppressWarnings("unchecked") private List<DesktopContextualMenu.Recipe> entries() {
        return (List<DesktopContextualMenu.Recipe>)field(wheel,"indexedEntries");
    }
    private ContextualWheelSelection selection() { return (ContextualWheelSelection)field(wheel,"selection"); }
    private void aimRecipe(Item item) {
        var list=entries();int index=-1;for(int i=0;i<list.size();i++) if(list.get(i).output().is(item) && list.get(i).craftable()) {index=i;break;}
        if(index<0) throw new IllegalStateException("Expected craftable native recipe for "+item);
        double angle=ContextualWheelSelection.outerMarkerAngle(index,list.size());
        h.rightStick((float)Math.sin(angle),(float)-Math.cos(angle));
    }
    private boolean selected(Item item) {
        if(wheel==null || entries().isEmpty()) return false;
        int index=selection().displayedSelected(entries().size());return index>=0 && entries().get(index).output().is(item);
    }
    static Object field(Object target,String name) {
        if(target==null) return null;
        for(Class<?> type=target instanceof Class<?> clazz?clazz:target.getClass();type!=null;type=type.getSuperclass()) try {
            Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(target instanceof Class<?>?null:target);
        } catch(NoSuchFieldException ignored) {} catch(ReflectiveOperationException failure) {throw new IllegalStateException(failure);}
        throw new IllegalStateException("Missing native observation "+name);
    }
    static Object invoke(Object target,String name) {
        if(target==null) return false;
        try {Method m=target.getClass().getMethod(name);return m.invoke(target);}
        catch(ReflectiveOperationException failure) {throw new IllegalStateException(failure);}
    }
    private static void type(Screen screen,char value) {
        try {
            try {screen.getClass().getMethod("charTyped",char.class,int.class).invoke(screen,value,0);return;}
            catch(NoSuchMethodException modern) {
                for(Method method:screen.getClass().getMethods()) if(method.getName().equals("charTyped") && method.getParameterCount()==1) {
                    Class<?> event=method.getParameterTypes()[0];
                    Object input=event.getConstructor(int.class,int.class).newInstance((int)value,0);
                    method.invoke(screen,input);return;
                }
                throw modern;
            }
        } catch(ReflectiveOperationException failure) {throw new IllegalStateException("Native keyboard typing",failure);}
    }
}
