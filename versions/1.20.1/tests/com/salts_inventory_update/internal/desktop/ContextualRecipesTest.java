package com.salts_inventory_update.internal.desktop;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class ContextualRecipesTest {
    @BeforeAll static void bootstrap(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    @Test void oneByOneShapedRecipeOccupiesTheNativeTableCenter(){
        var recipe=new ShapedRecipe(new ResourceLocation("test:single"),"",CraftingBookCategory.MISC,1,1,
            NonNullList.of(Ingredient.EMPTY,Ingredient.of(Items.OAK_LOG)),new ItemStack(Items.OAK_PLANKS,4));
        var grid=ContextualRecipes.grid(new CraftingMenu(1,new Inventory(null)),recipe);
        assertEquals(9,grid.size());
        assertTrue(grid.get(4).test(new ItemStack(Items.OAK_LOG)));
        assertEquals(8,grid.stream().filter(Ingredient::isEmpty).count());
    }
    @Test void threeWideRecipeNeverEntersCharacterCrafting(){
        var recipe=new ShapedRecipe(new ResourceLocation("test:wide"),"",CraftingBookCategory.MISC,3,1,
            NonNullList.of(Ingredient.EMPTY,Ingredient.of(Items.OAK_PLANKS),Ingredient.of(Items.OAK_PLANKS),Ingredient.of(Items.OAK_PLANKS)),new ItemStack(Items.OAK_SLAB,6));
        assertTrue(ContextualRecipes.grid(new InventoryMenu(new Inventory(null),true,null),recipe).isEmpty());
    }
    @Test void repeatedShapelessIngredientsRetainSeparateNativeCells(){
        var recipe=new ShapelessRecipe(new ResourceLocation("test:two"),"",CraftingBookCategory.MISC,new ItemStack(Items.STICK),
            NonNullList.of(Ingredient.EMPTY,Ingredient.of(Items.OAK_LOG),Ingredient.of(Items.OAK_LOG)));
        var grid=ContextualRecipes.grid(new CraftingMenu(1,new Inventory(null)),recipe);
        assertTrue(grid.get(0).test(new ItemStack(Items.OAK_LOG)));assertTrue(grid.get(1).test(new ItemStack(Items.OAK_LOG)));
        assertEquals(2,grid.stream().filter(ingredient->!ingredient.isEmpty()).count());
    }
}
