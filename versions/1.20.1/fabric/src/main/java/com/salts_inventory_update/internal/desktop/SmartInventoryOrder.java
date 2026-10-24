package com.salts_inventory_update.internal.desktop;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** A per-sort snapshot: no localized tooltips, mutable global comparator state or item mutation. */
public final class SmartInventoryOrder {
    private static final List<String> CATEGORIES = List.of("valuables", "equipment", "supplies", "crafting", "building", "terrain");
    private static final List<String> RESOURCES = List.of("gems", "ingots", "nuggets", "raw_materials", "ores", "storage_blocks");
    private static final List<String> TERRAIN = List.of("dirt", "gravels", "sands", "cobblestones", "stones", "netherracks", "end_stones");
    private static final List<String> SHAPES = List.of("_fence_gate", "_pressure_plate", "_hanging_sign", "_trapdoor", "_stairs", "_slab", "_planks", "_fence", "_button", "_door", "_sign", "_wood", "_log", "_stem", "_hyphae");
    private final Map<Item, CreativePosition> creative = new IdentityHashMap<>();

    public SmartInventoryOrder() {
        int index = 0;
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            if (tab.getType() != CreativeModeTab.Type.CATEGORY) continue;
            String group = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).getPath();
            for (ItemStack stack : tab.getDisplayItems()) {
                creative.putIfAbsent(stack.getItem(), new CreativePosition(group, index++));
            }
        }
    }

    public Comparator<ItemStack> comparator() {
        // Classification is evaluated once per distinct stack object, rather than scanning tags in comparisons.
        Map<ItemStack, Key> keys = new IdentityHashMap<>();
        return Comparator.comparing(stack -> keys.computeIfAbsent(stack, this::key));
    }

    private Key key(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        Set<String> tags = new HashSet<>();
        stack.getTags().forEach(tag -> tags.add(tag.location().toString()));
        if (stack.getItem() instanceof BlockItem block) {
            block.getBlock().defaultBlockState().getTags().forEach(tag -> tags.add(tag.location().toString()));
        }
        CreativePosition position = creative.getOrDefault(stack.getItem(), new CreativePosition("", Integer.MAX_VALUE));
        int resource = resourceGroup(tags, path);
        int category = -1;
        for (int i = 0; i < CATEGORIES.size(); i++) {
            if (tags.contains("salts_inventory_update:sort/" + CATEGORIES.get(i))) { category = i; break; }
        }
        if (category < 0) {
            if (resource >= 0) category = 0;
            else if ((stack.getItem() instanceof DiggerItem) || (stack.getItem() instanceof ArmorItem)
                || (stack.getItem() instanceof SwordItem) || (stack.getItem() instanceof EnchantedBookItem)
                || common(tags, "tools", "armors", "ranged_weapon_tools") || position.group.equals("combat")) category = 1;
            else if (stack.isEdible() || (stack.getItem() instanceof PotionItem)
                || position.group.equals("food_and_drinks")) category = 2;
            else if (terrain(tags, path)) category = 5;
            else if (position.group.equals("functional_blocks") || position.group.equals("redstone_blocks")) category = 3;
            else if (stack.getItem() instanceof BlockItem) category = 4;
            else category = 3;
        }
        String family = family(tags, path, category, resource);
        int enchantments = EnchantmentHelper.getEnchantments(stack).values().stream().mapToInt(Integer::intValue).sum();
        var food = stack.getItem().getFoodProperties();
        return new Key(category, category == 0 ? Math.max(0, resource) : 0, family,
            category == 1 ? -stack.getMaxDamage() : 0, category == 1 ? -enchantments : 0,
            category == 1 ? stack.getDamageValue() : 0, category == 2 && food != null ? -food.getNutrition() : 0,
            position.index, id, stack.hasCustomHoverName() ? stack.getHoverName().getString() : "");
    }

    private static int resourceGroup(Set<String> tags, String path) {
        for (int i = 0; i < RESOURCES.size(); i++) if (common(tags, RESOURCES.get(i))) return i;
        // Conservative identifier fallback for mods that omit common tags; do not scan translated names.
        if (path.endsWith("_gem")) return 0;
        if (path.endsWith("_ingot")) return 1;
        if (path.endsWith("_nugget")) return 2;
        if (path.endsWith("_ore")) return 4;
        return -1;
    }

    private static boolean common(Set<String> tags, String... groups) {
        for (String group : groups) {
            for (String tag : tags) {
                if (tag.equals("c:" + group) || tag.startsWith("c:" + group + "/")
                    || tag.equals("forge:" + group) || tag.startsWith("forge:" + group + "/")) return true;
            }
        }
        return false;
    }

    private static boolean terrain(Set<String> tags, String path) {
        return common(tags, TERRAIN.toArray(String[]::new)) || tags.contains("minecraft:dirt")
            || tags.contains("minecraft:sand") || tags.contains("minecraft:base_stone_overworld")
            || tags.contains("minecraft:base_stone_nether")
            || Set.of("dirt", "gravel", "sand", "red_sand", "cobblestone", "mossy_cobblestone", "stone", "netherrack", "end_stone").contains(path);
    }

    private static String family(Set<String> tags, String path, int category, int resource) {
        if (category == 0 && resource >= 0) {
            String group = RESOURCES.get(resource);
            return tags.stream().filter(tag -> tag.startsWith("c:" + group + "/") || tag.startsWith("forge:" + group + "/"))
                .map(tag -> tag.substring(tag.indexOf('/') + 1)).sorted().findFirst().orElse(path);
        }
        if (category == 1) {
            for (String type : List.of("pickaxes", "axes", "shovels", "hoes", "swords", "helmets", "chestplates", "leggings", "boots")) {
                if (tags.contains("minecraft:" + type) || common(tags, "tools/" + type, "armors/" + type)
                    || path.endsWith("_" + type.substring(0, type.length() - 1))) return type;
            }
            return path;
        }
        if (category == 4) {
            // Group a material's logs, planks and shaped blocks even if a mod puts them in separate tabs.
            String material = path.startsWith("stripped_") ? path.substring(9) : path;
            for (String shape : SHAPES) if (material.endsWith(shape)) return material.substring(0, material.length() - shape.length());
            for (String type : List.of("wools", "terracottas", "concretes", "stained_glass")) if (common(tags, type)) return type;
        }
        if (category == 5) {
            if (tags.contains("minecraft:dirt") || path.endsWith("dirt") || path.equals("dirt")) return "0_soil";
            for (int i = 0; i < TERRAIN.size(); i++) if (common(tags, TERRAIN.get(i))) return i + "_" + TERRAIN.get(i);
            if (path.contains("sand")) return "2_sand";
            if (path.contains("gravel")) return "1_gravel";
            return "4_stone";
        }
        return "";
    }

    private record CreativePosition(String group, int index) {}
    private record Key(int category, int subgroup, String family, int tier, int enchantments, int damage,
                       int food, int creative, String id, String name) implements Comparable<Key> {
        private static final Comparator<Key> ORDER = Comparator.comparingInt(Key::category).thenComparingInt(Key::subgroup)
            .thenComparing(Key::family).thenComparingInt(Key::tier).thenComparingInt(Key::enchantments)
            .thenComparingInt(Key::damage).thenComparingInt(Key::food).thenComparingInt(Key::creative)
            .thenComparing(Key::id).thenComparing(Key::name);
        @Override public int compareTo(Key other) { return ORDER.compare(this, other); }
    }
}
