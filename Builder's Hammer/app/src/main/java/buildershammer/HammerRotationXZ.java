package buildershammer;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockSoundEvent;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocksound.config.BlockSoundSet;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.VariantRotation;
import com.hypixel.hytale.server.core.asset.type.gameplay.GameplayConfig;
import com.hypixel.hytale.server.core.asset.type.gameplay.WorldConfig;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class HammerRotationXZ extends SimpleBlockInteraction {
    //Used as one part to register the interaction
    public static final BuilderCodec<HammerRotationXZ> CODEC = BuilderCodec.builder(
            HammerRotationXZ.class, HammerRotationXZ::new, SimpleBlockInteraction.CODEC
    ).build();

    //Largely referenced/copied from CycleBlockGroupInteraction.json
    @Override
    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> cmdBuffer,
        @Nonnull InteractionType intType, @Nonnull InteractionContext intCxt, @Nullable ItemStack itmStk,
        @Nonnull Vector3i blockPos, @Nonnull CooldownHandler cooldownHndlr) {
        //CustomRotationXZ interaction code here
        Ref<EntityStore> ref = intCxt.getEntity();
        Store<EntityStore> store = ref.getStore();
        Player playerComponent = cmdBuffer.getComponent(ref, Player.getComponentType());

        InteractionSyncData state = intCxt.getState();
        state.state = InteractionState.Failed;
        
        if (playerComponent == null) {
        (HytaleLogger.getLogger().at(Level.INFO)
         .atMostEvery(5, TimeUnit.MINUTES)).log("CustomRotationXZ requires a Player but was used for: %s", ref);
         return;
        }
        
        ChunkStore chkStore = world.getChunkStore();
        Store<ChunkStore> chkStoreStore = chkStore.getStore();

        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z);
        Ref<ChunkStore> chunkReference = chkStore.getChunkReference(chunkIndex);
        if (chunkReference == null || !chunkReference.isValid()) return;

        WorldChunk worldChunkComponent = chkStoreStore.getComponent(chunkReference, WorldChunk.getComponentType());
        assert worldChunkComponent != null;
     
        BlockChunk blockChunkComponent = chkStoreStore.getComponent(chunkReference, BlockChunk.getComponentType());
        assert blockChunkComponent != null;

        //Make sure player can change/edit blocks first
        GameplayConfig gameplayConfig = world.getGameplayConfig();
        WorldConfig worldConfig = gameplayConfig.getWorldConfig();
        
        boolean blockBreakingAllowed = worldConfig.isBlockBreakingAllowed();
        if (!blockBreakingAllowed) return;

        int blockIndex = world.getBlock(blockPos);
        BlockType targetBlockType = BlockType.getAssetMap().getAsset(blockIndex);
        if (targetBlockType == null) {
            return;
        }
        
        //VariantRotation varRot = targetBlockType.getVariantRotation();
        //if(varRot.equals(VariantRotation.None)) {
            //No rotation to be done, stuff like grass's texture will bug out if we rotate it like this
            //return;
        //}

        //Remove durability from the held item, pulled from vanilla hammer, doesn't work?
        ItemStack heldItem = intCxt.getHeldItem();
        if (heldItem != null && playerComponent.canDecreaseItemStackDurability(ref, (ComponentAccessor)store) && !heldItem.isUnbreakable()) {
            playerComponent.updateItemStackDurability(ref, heldItem, playerComponent.getInventory().getHotbar(), intCxt.getHeldItemSlot(), -heldItem.getItem().getDurabilityLossOnHit(), (ComponentAccessor)cmdBuffer);
        }
        //!!! Deprecated method getRotationIndex, marked for removal
        int rotation = worldChunkComponent.getRotationIndex(blockPos.x, blockPos.y, blockPos.z);
        if (rotation < 4){
            rotation = (rotation + 4); //Move rotation to a sideways position
        } else if (rotation >= 4 && rotation < 8){
            rotation = 8; //Move rotation to upside down position
        }else{
            rotation = 0; //Reset rotation to normal position
        }
        int blockID = BlockType.getAssetMap().getIndex(targetBlockType.getId());
        //The function I need to set the block with new rotation
        worldChunkComponent.setBlock(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockID, targetBlockType, rotation, 0, 256);
        state.state = InteractionState.Finished;

        //Add sound when editing the block, pulled from CycleBlockGroup interaction
        BlockSoundSet soundSet = BlockSoundSet.getAssetMap().getAsset(targetBlockType.getBlockSoundSetIndex());    
        if (soundSet != null) {
            int soundEventIndex = soundSet.getSoundEventIndices().getOrDefault(BlockSoundEvent.Hit, 0);
            if (soundEventIndex != 0) {
                SoundUtil.playSoundEvent3d(ref, soundEventIndex, blockPos.x + 0.5D, blockPos.y + 0.5D, blockPos.z + 0.5D, (ComponentAccessor)cmdBuffer);
            }
        } 
    }
    
    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType intType, @Nonnull InteractionContext intCxt,
            @Nullable ItemStack itmStk, @Nonnull World world, @Nonnull Vector3i blockPos) {
                //Needed to be overridden, but not used in this interaction
            }
}