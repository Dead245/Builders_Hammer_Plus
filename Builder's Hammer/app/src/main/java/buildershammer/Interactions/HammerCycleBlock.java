package buildershammer.Interactions;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3i;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.BlockGroup;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.BlockSoundEvent;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocksound.config.BlockSoundSet;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import java.util.logging.Level;

// Mostly yoinked from CycleBlockGroupInteraction.java, then updated with non deprecated methods and to work as a hammer function.
public class HammerCycleBlock extends SimpleBlockInteraction {
            //Used as one part to register the interaction
    public static final BuilderCodec<HammerCycleBlock> CODEC = BuilderCodec.builder(
            HammerCycleBlock.class, HammerCycleBlock::new, SimpleBlockInteraction.CODEC
    ).build();

    @Override
    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> cmdBuffer,
            @Nonnull InteractionType intType, @Nonnull InteractionContext intContext, @Nullable ItemStack itmStk,
            @Nonnull Vector3i blockPos, @Nonnull CooldownHandler cooldownHndlr) {

        final var ref = intContext.getEntity();
        //final var store = ref.getStore(); -- Not used but CycleBlockGroupInteraction.java had it so I left it here for now.
        final var playerComponent = cmdBuffer.getComponent(ref, Player.getComponentType());

        final var state = intContext.getState();
        state.state = InteractionState.Failed;

        if (playerComponent == null) { // TODO: Add LivingEntity support
            HytaleLogger.getLogger().at(Level.INFO)
                .atMostEvery(5, TimeUnit.MINUTES)
                .log("CycleBlockGroupInteraction requires a Player but was used for: %s", ref);
            return;
        }

        final var chunkStore = world.getChunkStore();
        final var chunkStoreStore = chunkStore.getStore();

        final var sectionRef = chunkStore.getChunkSectionReferenceAtBlock(blockPos.x, blockPos.y, blockPos.z);
        if (sectionRef == null || !sectionRef.isValid()) return;

        final var blockSection = chunkStoreStore.getComponent(sectionRef, BlockSection.getComponentType());
        if (blockSection == null) return;

        final var gameplayConfig = world.getGameplayConfig();
        final var worldConfig = gameplayConfig.getWorldConfig();

        final boolean blockBreakingAllowed = worldConfig.isBlockBreakingAllowed();
        if (!blockBreakingAllowed) return;

        final var blockIndex = blockSection.get(blockPos.x, blockPos.y, blockPos.z);
        final var targetBlockType = BlockType.getAssetMap().getAsset(blockIndex);

        if (targetBlockType == null) return;

        final var targetBlockItem = targetBlockType.getItem();

        // TODO BlockGroup is deprecated, needs replacement once system is migrated in vanilla.
        final var set = BlockGroup.findItemGroup(targetBlockItem);

        if (set == null) return;

        final var currentIndex = set.getIndex(targetBlockItem);
        if (currentIndex == -1) return;

        // Can change this in the future to use a variable on the interaction on what direction to use instead
        int rotationDirection = 0;
        switch (intType) {
            case Primary:
                rotationDirection = 1;
                break;
            case Secondary:
                rotationDirection = -1;
                break;
            default:
                rotationDirection = 1;
                break;
        }
        
        final var nextBlockKey = set.get(Math.floorMod(currentIndex + rotationDirection, set.size()));
        final var nextBlockType = BlockType.getAssetMap().getAsset(nextBlockKey);
        if (nextBlockType == null) return;

        // Decrement durability for the held item if applicable
        final var heldItem = intContext.getHeldItem();
        final var hotbarComponent = cmdBuffer.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (heldItem != null && hotbarComponent != null && ItemUtils.canDecreaseItemStackDurability(ref, cmdBuffer) && !heldItem.isUnbreakable()) {
            ItemUtils.updateItemStackDurability(ref, heldItem, hotbarComponent.getInventory(), intContext.getHeldItemSlot(), -heldItem.getItem().getDurabilityLossOnHit(), cmdBuffer);
        }

        var newBlockId = BlockType.getAssetMap().getIndex(nextBlockType.getId());
        var rotation = blockSection.getRotationIndex(blockPos.x, blockPos.y, blockPos.z);
        BlockOperations.setBlock(chunkStore, sectionRef, blockPos.x(), blockPos.y(), blockPos.z(), newBlockId, nextBlockType, rotation, FillerBlockUtil.NO_FILLER, SetBlockSettings.PERFORM_BLOCK_UPDATE);
        state.state = InteractionState.NotFinished;

        final var soundSet = BlockSoundSet.getAssetMap().getAsset(nextBlockType.getBlockSoundSetIndex());

        if (soundSet != null) {
            final int soundEventIndex = soundSet.getSoundEventIndices().getOrDefault(BlockSoundEvent.Hit, SoundEvent.EMPTY_ID);
            if (soundEventIndex != SoundEvent.EMPTY_ID) {
                SoundUtil.playSoundEvent3d(ref, soundEventIndex, blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5, cmdBuffer);
            }
        }
    }

    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType intType, @Nonnull InteractionContext intContext,
            @Nullable ItemStack itmStk, @Nonnull World world, @Nonnull Vector3i blockPos) {
        // Nothing needs to be simulated here
    }
    
}
