package buildershammer.Interactions;

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
import com.hypixel.hytale.protocol.BlockSoundEvent;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocksound.config.BlockSoundSet;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
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
import org.joml.Vector3i;

import buildershammer.BuildersHammer;
import buildershammer.Helpers.RotationFunctions;

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
        
        //Get config values
        BuildersHammer bHammer = BuildersHammer.getInstance();
        boolean permission = bHammer.canEdit(worldChunkComponent.getBlockType(blockPos).getId(), playerComponent.getGameMode().name(), "");
        if(!permission) return;

        boolean blockBreakingAllowed = worldConfig.isBlockBreakingAllowed();
        if (!blockBreakingAllowed) return;

        int blockIndex = world.getBlock(blockPos);
        BlockType targetBlockType = BlockType.getAssetMap().getAsset(blockIndex);
        if (targetBlockType == null) {
            return;
        }

        // TODO - Refactor to take into account the face the player clicks on, and rotate the block accordingly.
        // TODO If hammer is in a certain mode, toggleable with E by default, swap between normal rotation or Axis Locked rotation.

        //int rotation = RotationFunctions.rotateBlockOrientation(intType, world, blockPos);
        int rotation = RotationFunctions.rotateBlockFacing(intType, world, blockPos);


        //TODO - newRoot for future use - for changing the blockPos due to rotating larger objects. not implemented yet
        Vector3i newRoot = blockPos;

        int blockID = BlockType.getAssetMap().getIndex(targetBlockType.getId());

        //The function I need to set the block with new rotation
        worldChunkComponent.setBlock(newRoot.x, newRoot.y, newRoot.z, blockID, targetBlockType, rotation, 0, 256);
        state.state = InteractionState.Finished;
        
        //Add sound when editing the block, pulled from CycleBlockGroup interaction
        BlockSoundSet soundSet = BlockSoundSet.getAssetMap().getAsset(targetBlockType.getBlockSoundSetIndex());    
        if (soundSet != null) {
            int soundEventIndex = soundSet.getSoundEventIndices().getOrDefault(BlockSoundEvent.Hit, 0);
            if (soundEventIndex != 0) {
                SoundUtil.playSoundEvent3d(ref, soundEventIndex, newRoot.x + 0.5D, newRoot.y + 0.5D, newRoot.z + 0.5D, (ComponentAccessor)cmdBuffer);
            }
        } 
    }
    
    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType intType, @Nonnull InteractionContext intCxt,
            @Nullable ItemStack itmStk, @Nonnull World world, @Nonnull Vector3i blockPos) {
                //Needed to be overridden, but not used in this interaction
            }
}