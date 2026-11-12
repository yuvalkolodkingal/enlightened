package com.kolodkingal.enlightened.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

/**
 * 1.21.11 only: NeoForge 21.11 saves an attachment through a {@link MapCodec} into a compound of its own, where 21.1
 * took a {@link Codec}. The mod's attachments keep the format they had (so a world keeps its bending when it moves
 * to 1.21.11).
 */
public final class AttachCodecs {
    private AttachCodecs() {
    }

    /** A record codec's map codec: the same fields in the same compound as before. */
    @SuppressWarnings("unchecked")
    public static <T> MapCodec<T> map(Codec<T> codec) {
        if (codec instanceof MapCodec.MapCodecCodec<?> m) {
            return (MapCodec<T>) m.codec();
        }
        throw new IllegalArgumentException("not a record codec: " + codec);
    }

    /** An item stack, possibly empty (nothing is saved for an empty one; an empty compound reads as empty). */
    public static final IAttachmentSerializer<ItemStack> ITEM = new IAttachmentSerializer<>() {
        @Override
        public ItemStack read(IAttachmentHolder holder, ValueInput input) {
            return input.read(ItemStack.MAP_CODEC).orElse(ItemStack.EMPTY);
        }

        @Override
        public boolean write(ItemStack stack, ValueOutput output) {
            if (stack.isEmpty()) {
                return false;
            }
            output.store(ItemStack.MAP_CODEC, stack);
            return true;
        }
    };
}
