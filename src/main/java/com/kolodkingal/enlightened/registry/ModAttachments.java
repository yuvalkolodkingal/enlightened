package com.kolodkingal.enlightened.registry;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.BendingData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Enlightened.MODID);

    /** A player's bending. Saved, kept on death, synced to everyone who can see the player. */
    public static final Supplier<AttachmentType<BendingData>> BENDING = ATTACHMENTS.register("bending",
            () -> AttachmentType.builder(BendingData::new)
                    //#if >=1.21.11
                    //$$ .serialize(com.kolodkingal.enlightened.compat.AttachCodecs.map(BendingData.CODEC))
                    //#else
                    .serialize(BendingData.CODEC)
                    //#endif
                    .copyOnDeath()
                    //#if <1.21.4
                    .sync(BendingData.STREAM_CODEC)
                    //#endif
                    .build());

    /** What a player wears on his back (the back slot). Saved, kept on death (dropped there unless keepInventory), synced. */
    public static final Supplier<AttachmentType<net.minecraft.world.item.ItemStack>> BACK = ATTACHMENTS.register("back",
            () -> AttachmentType.builder(() -> net.minecraft.world.item.ItemStack.EMPTY)
                    //#if >=1.21.11
                    //$$ .serialize(com.kolodkingal.enlightened.compat.AttachCodecs.ITEM)
                    //#else
                    .serialize(net.minecraft.world.item.ItemStack.OPTIONAL_CODEC)
                    //#endif
                    .copyOnDeath()
                    //#if <1.21.4
                    .sync(net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC)
                    //#endif
                    .build());

    private ModAttachments() {
    }
}
