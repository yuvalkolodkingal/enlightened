package com.kolodkingal.enlightened.gametest;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.registry.ModItems;
import com.kolodkingal.enlightened.world.ElloraPiece;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.kolodkingal.enlightened.gametest.TestKit.ARENA;
import static com.kolodkingal.enlightened.gametest.TestKit.check;

/** The GameTests of Seven Havens: the brigade armor trim and the city of Ellora where it is found. */
@GameTestHolder(Enlightened.MODID)
@PrefixGameTestTemplate(false)
public final class SevenHavensTests {

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void theBrigadeTrimDressesAnyArmorInItsBrigade(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var access = level.registryAccess();
        ItemStack template = new ItemStack(ModItems.BRIGADE_TRIM_TEMPLATE.get());
        String[][] brigades = {{"lapis", "lapis"}, {"quartz", "quartz"}, {"redstone", "redstone"}, {"coal", "coal"}};
        Item[] ingredients = {Items.LAPIS_LAZULI, Items.QUARTZ, Items.REDSTONE, Items.COAL};
        for (int i = 0; i < ingredients.length; i++) {
            ItemStack material = new ItemStack(ingredients[i]);
            check(TrimMaterials.getFromIngredient(access, material).isPresent(), brigades[i][0] + " is a trim material");
            for (ItemStack armor : new ItemStack[]{new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.LEATHER_LEGGINGS),
                    new ItemStack(Items.NETHERITE_HELMET), new ItemStack(Items.CHAINMAIL_BOOTS)}) {
                SmithingRecipeInput input = new SmithingRecipeInput(template, armor, material);
                var recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level);
                check(recipe.isPresent(), "the smithing table trims " + armor.getItem() + " with " + brigades[i][0]);
                ArmorTrim trim = recipe.get().value().assemble(input, access).get(DataComponents.TRIM);
                check(trim != null && trim.pattern().is(Enlightened.id("brigade")), "in the brigade's pattern");
                String tex = trim.outerTexture(ArmorMaterials.IRON).getPath();
                check(tex.equals("trims/models/armor/brigade_" + brigades[i][1]), "drawn as the " + brigades[i][0] + " brigade (" + tex + ")");
            }
        }
        Holder<TrimMaterial> coal = TrimMaterials.getFromIngredient(access, new ItemStack(Items.COAL)).orElseThrow();
        check(coal.value().assetName().equals("coal"), "coal is its own palette");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void elloraIsAStarOfTerracesRoundAGoldenKeep(GameTestHelper helper) {
        check(ElloraPiece.design(0, ElloraPiece.KEEP_TOP, ElloraPiece.KEEP).is(Blocks.GOLD_BLOCK), "the keep has a golden rim");
        check(ElloraPiece.design(0, ElloraPiece.KEEP_TOP + 20, 0).is(Blocks.GOLD_BLOCK), "the golden spire rises from it");
        int gold = 0;
        for (int x = -24; x <= 24; x++) {
            for (int y = ElloraPiece.KEEP_TOP + 1; y < ElloraPiece.KEEP_TOP + 30; y++) {
                for (int z = -24; z <= 24; z++) {
                    BlockState s = ElloraPiece.design(x, y, z);
                    if (s != null && s.is(Blocks.GOLD_BLOCK) && (Math.abs(x) > 1 || Math.abs(z) > 1)) {
                        gold++;
                    }
                }
            }
        }
        check(gold > 150, "great golden crystals crown the keep (" + gold + " blocks)");
        check(ElloraPiece.design(0, ElloraPiece.PLAZA + 1, -ElloraPiece.KEEP + 2).isAir(), "room in the hall for the brigades' chest");
        check(ElloraPiece.design(0, ElloraPiece.PLAZA + 2, -ElloraPiece.KEEP).isAir(), "the keep has a gate");
        check(ElloraPiece.top(16, 8) > ElloraPiece.top(26, 18) && ElloraPiece.top(26, 18) > ElloraPiece.top(36, 28), "the arms step down outward ("
                + ElloraPiece.top(16, 8) + ", " + ElloraPiece.top(26, 18) + ", " + ElloraPiece.top(36, 28) + ")");
        check(ElloraPiece.top(60, 0) < 0, "and the city ends");
        // Every pool is held in on all sides: none spills down a terrace.
        for (int x = -53; x <= 53; x++) {
            for (int z = -53; z <= 53; z++) {
                int t = ElloraPiece.top(x, z);
                if (t >= 0 && ElloraPiece.design(x, t, z).is(Blocks.WATER)) {
                    for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        check(ElloraPiece.top(x + d[0], z + d[1]) == t, "a pool at " + x + "," + z + " is level with its neighbours");
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void elloraKeepsTheTemplateAndNoMonsters(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var loot = level.getServer().reloadableRegistries().getLootTable(ElloraPiece.LOOT_BRIGADE);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
        List<ItemStack> items = loot.getRandomItems(params);
        check(items.stream().anyMatch(s -> s.is(ModItems.BRIGADE_TRIM_TEMPLATE.get())), "the brigades' chest always holds the template");
        Structure ellora = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(Enlightened.id("ellora"));
        check(ellora != null, "Ellora is a structure");
        var monsters = ellora.spawnOverrides().get(MobCategory.MONSTER);
        check(monsters != null && monsters.spawns().isEmpty(), "no monsters spawn in Ellora");
        helper.succeed();
    }
}
