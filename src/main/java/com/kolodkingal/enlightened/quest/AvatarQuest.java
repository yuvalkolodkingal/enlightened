package com.kolodkingal.enlightened.quest;

import com.kolodkingal.enlightened.EnlightenedConfig;
import com.kolodkingal.enlightened.bending.AvatarCycle;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.entity.DarkSpiritEntity;
import com.kolodkingal.enlightened.entity.GreatSpiritEntity;
import com.kolodkingal.enlightened.fx.Fx;
import com.kolodkingal.enlightened.registry.ModEntities;
import com.kolodkingal.enlightened.world.SpiritWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * How one becomes the Avatar, as Wan did: by Raava.
 *
 * <p>In a world on the <b>quest</b> path (the default; {@code general.avatarPath}), nobody is born the Avatar.
 * Raava, the spirit of light, waits in glades about the world. Whoever finds her and agrees to help her against Vaatu,
 * the spirit of darkness who has broken free, she enters, and he becomes the Avatar. Then the dark spirits come for
 * him: when he has beaten {@link #SPIRITS} of them, Raava tells him Vaatu is in the Spirit World. There Vaatu speaks to
 * him and offers him to betray Raava and become the <b>Dark Avatar</b>. If he refuses, he must beat Vaatu; if he
 * accepts, Vaatu enters him, and he must beat Raava.
 *
 * <p>On the <b>fate</b> path, a newborn player is the Avatar one time in seven and the Dark Avatar one time in seven
 * (each while the world has room for one), and Raava and Vaatu are never found.
 *
 * <p>The stage is kept on the player (through deaths).
 */
public final class AvatarQuest {
    public enum Stage {
        /** Not begun. */
        NONE,
        /** Raava is in him: the dark spirits come. */
        SPIRITS,
        /** The spirits beaten: Vaatu waits in the Spirit World. */
        SEEK_VAATU,
        /** Vaatu has made his offer and waits for the answer. */
        OFFERED,
        /** He refused: he fights Vaatu. */
        FIGHT_VAATU,
        /** He accepted: he is the Dark Avatar, and fights Raava. */
        FIGHT_RAAVA,
        /** He beat Vaatu. */
        DONE_LIGHT,
        /** He beat Raava. */
        DONE_DARK
    }

    /** Dark spirits to beat before Vaatu shows himself. */
    public static final int SPIRITS = 10;
    /** Dark spirits about him at once. */
    public static final int PACK = 4;
    private static final String KEY = "enlightened_quest";
    private static final String KILLS = "enlightened_quest_kills";

    // ---- the path the world is on ----

    public static boolean questPath() {
        return !EnlightenedConfig.SPEC.isLoaded() || EnlightenedConfig.AVATAR_PATH.get() == EnlightenedConfig.AvatarPath.QUEST;
    }

    /** Whether Raava may be found and may choose someone: the quest path, and room in the world for an Avatar. */
    public static boolean raavaFree(net.minecraft.server.MinecraftServer server) {
        return questPath() && !AvatarCycle.get(server).full();
    }

    // ---- the stage ----

    private static CompoundTag kept(Player p) {
        return p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static Stage stage(Player p) {
        Stage[] all = Stage.values();
        return all[Math.floorMod(kept(p).getInt(KEY), all.length)];
    }

    public static void setStage(Player p, Stage stage) {
        CompoundTag tag = kept(p);
        tag.putInt(KEY, stage.ordinal());
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, tag);
    }

    public static int kills(Player p) {
        return kept(p).getInt(KILLS);
    }

    private static void setKills(Player p, int n) {
        CompoundTag tag = kept(p);
        tag.putInt(KILLS, n);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, tag);
    }

    // ---- Raava's offer ----

    /** Raava agreed with: she enters him and he is the Avatar; the dark spirits will come. Returns false if she may not. */
    public static boolean merge(ServerPlayer p, GreatSpiritEntity raava) {
        if (!raavaFree(p.getServer()) || Benders.get(p).element() == Element.AVATAR) {
            return false;
        }
        ServerLevel level = p.serverLevel();
        Fx.send(level, Fx.ENERGYBEND, raava.position().add(0, 1, 0), p.position().add(0, 1, 0), 0xBFF4FF, 1.0F, p.getId());
        level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5F, 1.2F);
        raava.discard();
        AvatarCycle.becomeAvatar(p.getServer(), p);
        setStage(p, Stage.SPIRITS);
        setKills(p, 0);
        title(p, "message.enlightened.quest.merged", "message.enlightened.quest.merged.sub", ChatFormatting.AQUA);
        p.sendSystemMessage(Component.translatable("message.enlightened.quest.spirits_come").withStyle(ChatFormatting.AQUA));
        return true;
    }

    // ---- every second ----

    public static void tick(ServerPlayer p) {
        if (p.tickCount % 20 != 7 || p.isSpectator()) {
            return;
        }
        Stage stage = stage(p);
        ServerLevel level = p.serverLevel();
        switch (stage) {
            case SPIRITS -> {
                if (level.dimension() == SpiritWorld.LEVEL) {
                    return;
                }
                List<DarkSpiritEntity> near = level.getEntitiesOfClass(DarkSpiritEntity.class, p.getBoundingBox().inflate(40),
                        s -> p.getUUID().equals(s.hunter()));
                if (near.size() < PACK && level.random.nextInt(4) == 0) {
                    spawnSpirit(level, p);
                }
            }
            case SEEK_VAATU, OFFERED, FIGHT_VAATU -> {
                if (level.dimension() != SpiritWorld.LEVEL) {
                    return;
                }
                GreatSpiritEntity vaatu = spirit(level, p, GreatSpiritEntity.Kind.VAATU);
                if (vaatu == null) {
                    vaatu = summonSpirit(level, p, GreatSpiritEntity.Kind.VAATU, stage == Stage.FIGHT_VAATU);
                    if (stage == Stage.SEEK_VAATU || stage == Stage.OFFERED) {
                        setStage(p, Stage.OFFERED);
                        offer(p);
                    }
                }
            }
            case FIGHT_RAAVA -> {
                if (level.dimension() == SpiritWorld.LEVEL && spirit(level, p, GreatSpiritEntity.Kind.RAAVA) == null) {
                    summonSpirit(level, p, GreatSpiritEntity.Kind.RAAVA, true);
                }
            }
            default -> {
            }
        }
    }

    /** A dark spirit comes for him, out of the dark a little way off. */
    static void spawnSpirit(ServerLevel level, ServerPlayer p) {
        DarkSpiritEntity s = ModEntities.DARK_SPIRIT.get().create(level);
        if (s == null) {
            return;
        }
        double a = level.random.nextDouble() * Math.PI * 2;
        double r = 10 + level.random.nextDouble() * 6;
        int x = (int) Math.floor(p.getX() + Math.cos(a) * r);
        int z = (int) Math.floor(p.getZ() + Math.sin(a) * r);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (Math.abs(y - p.getY()) > 12) {
            y = (int) p.getY();
        }
        s.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        s.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, y, z)), MobSpawnType.EVENT, null);
        s.hunt(p);
        level.addFreshEntity(s);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, s.getX(), s.getY() + 0.5, s.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
    }

    /** A dark spirit that hunted him is beaten. */
    public static void spiritBeaten(ServerPlayer p) {
        if (stage(p) != Stage.SPIRITS) {
            return;
        }
        int n = kills(p) + 1;
        setKills(p, n);
        if (n >= SPIRITS) {
            setStage(p, Stage.SEEK_VAATU);
            for (DarkSpiritEntity s : p.serverLevel().getEntitiesOfClass(DarkSpiritEntity.class, p.getBoundingBox().inflate(48),
                    s -> p.getUUID().equals(s.hunter()))) {
                s.discard();
            }
            title(p, "message.enlightened.quest.vaatu_waits", "message.enlightened.quest.vaatu_waits.sub", ChatFormatting.AQUA);
            p.sendSystemMessage(Component.translatable("message.enlightened.quest.raava_speaks").withStyle(ChatFormatting.AQUA));
        } else {
            p.displayClientMessage(Component.translatable("message.enlightened.quest.spirit_beaten", n, SPIRITS).withStyle(ChatFormatting.AQUA), true);
        }
    }

    // ---- Vaatu ----

    static GreatSpiritEntity spirit(ServerLevel level, ServerPlayer p, GreatSpiritEntity.Kind kind) {
        List<GreatSpiritEntity> list = level.getEntitiesOfClass(GreatSpiritEntity.class, new AABB(p.blockPosition()).inflate(96),
                s -> s.kind() == kind && p.getUUID().equals(s.chosen()));
        return list.isEmpty() ? null : list.get(0);
    }

    static GreatSpiritEntity summonSpirit(ServerLevel level, ServerPlayer p, GreatSpiritEntity.Kind kind, boolean hostile) {
        GreatSpiritEntity s = ModEntities.GREAT_SPIRIT.get().create(level);
        if (s == null) {
            return null;
        }
        Vec3 ahead = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(12)).add(0, 4, 0);
        s.moveTo(ahead.x, ahead.y, ahead.z, p.getYRot() + 180.0F, 0.0F);
        s.setup(kind, p, hostile);
        level.addFreshEntity(s);
        level.playSound(null, s.blockPosition(), kind == GreatSpiritEntity.Kind.VAATU ? SoundEvents.WARDEN_EMERGE : SoundEvents.BEACON_POWER_SELECT,
                SoundSource.HOSTILE, 2.0F, 0.7F);
        return s;
    }

    /** Vaatu's offer, with the two answers to click. */
    static void offer(ServerPlayer p) {
        title(p, "message.enlightened.quest.vaatu", "message.enlightened.quest.vaatu.sub", ChatFormatting.DARK_RED);
        p.sendSystemMessage(Component.translatable("message.enlightened.quest.vaatu_offer").withStyle(ChatFormatting.DARK_RED));
        Component join = Component.translatable("message.enlightened.quest.answer_dark").withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/avatarquest dark"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("message.enlightened.quest.answer_dark.hover"))));
        Component refuse = Component.translatable("message.enlightened.quest.answer_light").withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/avatarquest light"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("message.enlightened.quest.answer_light.hover"))));
        p.sendSystemMessage(Component.empty().append(join).append("   ").append(refuse));
    }

    /**
     * His answer to Vaatu: refuse (fight Vaatu) or join him (become the Dark Avatar and fight Raava). Returns the key
     * of what he is told (null if he was not asked).
     */
    public static String answer(ServerPlayer p, boolean dark) {
        if (stage(p) != Stage.OFFERED) {
            return "message.enlightened.quest.not_asked";
        }
        ServerLevel level = p.serverLevel();
        GreatSpiritEntity vaatu = spirit(level, p, GreatSpiritEntity.Kind.VAATU);
        if (!dark) {
            setStage(p, Stage.FIGHT_VAATU);
            if (vaatu != null) {
                vaatu.turnHostile(p);
            }
            title(p, "message.enlightened.quest.refused", "message.enlightened.quest.refused.sub", ChatFormatting.AQUA);
            return "message.enlightened.quest.refused.sub";
        }
        becomeDark(p);
        if (vaatu != null) {
            Fx.send(level, Fx.ENERGYBEND, vaatu.position().add(0, 1, 0), p.position().add(0, 1, 0), 0x8B0A12, 1.0F, p.getId());
            vaatu.discard();
        }
        setStage(p, Stage.FIGHT_RAAVA);
        summonSpirit(level, p, GreatSpiritEntity.Kind.RAAVA, true);
        title(p, "message.enlightened.quest.dark", "message.enlightened.quest.dark.sub", ChatFormatting.DARK_RED);
        return "message.enlightened.quest.dark.sub";
    }

    /** He becomes the Dark Avatar: the darkness takes him; he is free of every tether. */
    public static void becomeDark(ServerPlayer p) {
        BendingData data = Benders.get(p);
        if (data.element() != Element.AVATAR) {
            AvatarCycle.becomeAvatar(p.getServer(), p);
        }
        data.setDarkAvatar(true);
        data.setTetherless(true);
        AvatarCycle.get(p.getServer()).markDark(p.getUUID(), true);
        Benders.syncIfDirty(p);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 1.2F, 0.8F);
    }

    /** One of the great spirits is beaten by the one it was set against. */
    public static void spiritDefeated(ServerPlayer p, GreatSpiritEntity.Kind kind) {
        Stage stage = stage(p);
        if (kind == GreatSpiritEntity.Kind.VAATU && stage == Stage.FIGHT_VAATU) {
            setStage(p, Stage.DONE_LIGHT);
            title(p, "message.enlightened.quest.won_light", "message.enlightened.quest.won_light.sub", ChatFormatting.AQUA);
        } else if (kind == GreatSpiritEntity.Kind.RAAVA && stage == Stage.FIGHT_RAAVA) {
            setStage(p, Stage.DONE_DARK);
            title(p, "message.enlightened.quest.won_dark", "message.enlightened.quest.won_dark.sub", ChatFormatting.DARK_RED);
        }
    }

    static void title(ServerPlayer p, String title, String sub, ChatFormatting colour) {
        p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(title).withStyle(colour)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(sub)));
    }

    /** Whether this player was chosen by a great spirit (for the spirit's own checks). */
    public static boolean chosen(ServerPlayer p, UUID id) {
        return p.getUUID().equals(id);
    }

    private AvatarQuest() {
    }
}
