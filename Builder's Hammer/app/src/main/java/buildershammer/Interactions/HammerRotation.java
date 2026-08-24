package buildershammer.Interactions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.Axis;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.BlockSoundEvent;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Rotation;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.protocol.BlockFace;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.asset.type.blocksound.config.BlockSoundSet;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.gameplay.GameplayConfig;
import com.hypixel.hytale.server.core.asset.type.gameplay.WorldConfig;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockComponentSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;

import org.joml.Vector3i;

import buildershammer.BuildersHammer;
import buildershammer.Helpers.BlockFunctions;
import buildershammer.Helpers.RotationFunctions;

public class HammerRotation extends SimpleBlockInteraction {

    //Used as one part to register the interaction
    public static final BuilderCodec<HammerRotation> CODEC = BuilderCodec.builder(
            HammerRotation.class, HammerRotation::new, SimpleBlockInteraction.CODEC
    ).build();

    @Override
    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> cmdBuffer,
        @Nonnull InteractionType intType, @Nonnull InteractionContext intCxt, @Nullable ItemStack itmStk,
        @Nonnull Vector3i blockPos, @Nonnull CooldownHandler cooldownHndlr) {
        Ref<EntityStore> playerEntityRef = intCxt.getEntity();
        Player playerComponent = cmdBuffer.getComponent(playerEntityRef, Player.getComponentType());
        InteractionSyncData state = intCxt.getState();
        state.state = InteractionState.NotFinished;
        Store<EntityStore> entityStore = cmdBuffer.getStore();

        BlockFace face = intCxt.getClientState().blockFace;
        Axis clickAxis = RotationFunctions.getAxisFromFace(face);
        
        if (playerComponent == null) {
        (HytaleLogger.getLogger().at(Level.INFO)
         .atMostEvery(5, TimeUnit.MINUTES)).log("HammerRotation requires a Player but was used for: %s", playerEntityRef);
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
        
        Ref<ChunkStore> sectionReference = chkStore.getChunkSectionReferenceAtBlock(blockPos.x, blockPos.y, blockPos.z);
        if (sectionReference == null) {
            state.state = InteractionState.Failed;
            return;
        }
        BlockSection blockSection = chkStoreStore.getComponent(sectionReference, BlockSection.getComponentType());

        // Get the block ID, blockType, and stringID
        int blockID = blockSection.get(blockPos.x, blockPos.y, blockPos.z);
        BlockType blockType = BlockType.getAssetMap().getAsset(blockID);
        String stringID = blockType.getId();
        

        //Get config values
        BuildersHammer bHammer = BuildersHammer.getInstance();
        boolean permission = bHammer.canEdit(stringID, playerComponent.getGameMode().name(), "");
        if(!permission) return;

        boolean blockBreakingAllowed = worldConfig.isBlockBreakingAllowed();
        if (!blockBreakingAllowed) return;

        int blockIndex = world.getBlock(blockPos);
        BlockType targetBlockType = BlockType.getAssetMap().getAsset(blockIndex);
        if (targetBlockType == null) {
            return;
        }

        int direction = 0;
        switch (intType) {
            case InteractionType.Primary -> {
                // Cycle Forwards
                direction = switch (face) {
                    case BlockFace.Down,BlockFace.North, BlockFace.West -> 1;
                    case BlockFace.East, BlockFace.South, BlockFace.Up -> -1;
                    default -> 1;
                };
            }
            case InteractionType.Secondary -> {
                // Cycle Backwards
                direction = switch (face) {
                    case BlockFace.Down,BlockFace.North, BlockFace.West -> -1;
                    case BlockFace.East, BlockFace.South, BlockFace.Up -> 1;
                    default -> 1;
                };
            }
            default -> {
                direction = 1;
            }
        }
        
        // Get the setting from the item metadata, which determines how the block should be rotated.
        Integer interactionSetting = intCxt.getHeldItem().getFromMetadataOrNull("Setting", Codec.INTEGER);
        
        boolean forcePlace = false; // Default value for forcePlace, can be overridden by Setting 4

        int newRotation;
        switch (interactionSetting) {
            case null:
                // Default rotation
                newRotation = RotationFunctions.rotateBlockFacing(direction, world, blockPos, clickAxis);
                break;
            case 1:
                // Setting 1
                face = BlockFace.North; // "Axis Lock"
                clickAxis = RotationFunctions.getAxisFromFace(BlockFace.North);
                newRotation = RotationFunctions.rotateBlockFacing(direction, world, blockPos, clickAxis);
                break;
            case 2:
                // Setting 2
                face = BlockFace.West; // "2nd Axis Lock"
                clickAxis = RotationFunctions.getAxisFromFace(BlockFace.West);
                newRotation = RotationFunctions.rotateBlockFacing(direction, world, blockPos, clickAxis);
                break;
            case 3:
                // Setting 3
                face = BlockFace.Up; // "3rd Axis Lock"
                clickAxis = RotationFunctions.getAxisFromFace(BlockFace.Up);
                newRotation = RotationFunctions.rotateBlockFacing(direction, world, blockPos, clickAxis);
                break;
            case 4:
                // Setting 4
                forcePlace = true; // "Force Place"
                newRotation = RotationFunctions.rotateBlockFacing(direction, world, blockPos, clickAxis);
                break;
            default:
                // This should never happen, but if it does, default to normal rotation.
                newRotation = RotationFunctions.rotateBlockFacing(direction, world, blockPos, clickAxis);
                break;
        }

        if (newRotation == -1) {
            state.state = InteractionState.Failed;
            return;
        }

        Vector3i newRoot = blockPos;
        
        int oldRotation = RotationFunctions.getRotationTuple(world, blockPos).index();
        Set<Vector3i> oldFootprint = BlockFunctions.getFootprint(blockPos, blockID, oldRotation);

        //Multi-cell block check and edits
        if (FillerBlockUtil.multiCellFootprint(blockID, newRotation) != null) {
            // This means the block clicked is a multi-cell block, and we need to check if the entire footprint can be placed and etc.
            BlockPosition rawBlockPos = intCxt.getTargetBlockRaw();
            newRoot = RotationFunctions.getNewMulticellRoot(blockPos, rawBlockPos, clickAxis, direction);

            //Validate that the block can be rotated without breaking other blocks, unless forcePlace is true
            if (!forcePlace){
                boolean isValid = BlockOperations.testPlaceBlock(chkStoreStore, blockSection, newRoot.x, newRoot.y, newRoot.z, blockType, newRotation,
                (tx, ty, tz, bType, rot, fill) -> oldFootprint.contains(new Vector3i(tx, ty, tz)));

                if (!isValid) {
                    state.state = InteractionState.Failed;
                    return;
                }
            }
        }
        
        Set<Vector3i> newFootprint = BlockFunctions.getFootprint(newRoot, blockID, newRotation);

        // Check if the block has additional data in the BlockComponentSection that needs to be transferred
        BlockComponentSection blockCompSection = chkStoreStore.getComponent(sectionReference, BlockComponentSection.getComponentType());
        int oldChunkIndex = ChunkUtil.indexBlock(blockPos.x, blockPos.y, blockPos.z);
        Ref<ChunkStore> oldBlockRef = null;
        if (blockCompSection != null) {
            oldBlockRef = blockCompSection.getBlockReference(oldChunkIndex);
        }

        int settings;
        if (forcePlace) {
            settings = SetBlockSettings.NO_UPDATE_STATE | SetBlockSettings.NO_UPDATE_NEIGHBOR_CONNECTIONS;
        } else {
            settings = SetBlockSettings.PERFORM_BLOCK_UPDATE;
        }

        //Remove old block data

        if (oldBlockRef != null && blockCompSection != null) {
            for (Vector3i pos : oldFootprint) {
                int index = ChunkUtil.indexBlock(pos.x, pos.y, pos.z);
                
                // Clear from blockHolders map
                blockCompSection.removeBlockHolder(index); 
                
                // Clear from blockReferences map (Crucial to prevent entity deletion!)
                blockCompSection.removeBlockReference(index, oldBlockRef); 
            }
        }

        blockSection.set(blockPos.x, blockPos.y, blockPos.z, 0, 0, 0);
        FillerBlockUtil.removeFillerBlocksAt(chkStoreStore, blockSection, blockCompSection, blockPos.x, blockPos.y, blockPos.z, blockID, FillerBlockUtil.NO_FILLER,
            oldRotation, FillerBlockUtil.ChangeReason.NONE);
        
        List<Vector3i> blocksToOverride = new ArrayList<>();
        for (Vector3i pos : newFootprint) {
            if (!oldFootprint.contains(pos)) {
                blocksToOverride.add(pos);
            }
        }
        BlockHarvestUtils.performBlockBreak(playerEntityRef, null, blocksToOverride, 0, entityStore, chkStoreStore);

        world.sendMessage(Message.raw("Face: " + face + " - Axis: " + clickAxis + " - Setting: " + interactionSetting));
        
        Ref<ChunkStore> targetSectionRef = chkStore.getChunkSectionReferenceAtBlock(newRoot.x, newRoot.y, newRoot.z);
        if (targetSectionRef == null || !targetSectionRef.isValid()) {
            state.state = InteractionState.Failed;
            return;
        }

        // Finally add the new block and data to the world at the new position with the new rotation
        BlockOperations.setBlock(chkStore, targetSectionRef, newRoot.x, newRoot.y, newRoot.z, blockID, targetBlockType, newRotation, FillerBlockUtil.NO_FILLER, settings);

        if (oldBlockRef != null && oldBlockRef.isValid()) {
            BlockComponentSection newCompSection = chkStoreStore.getComponent(targetSectionRef, BlockComponentSection.getComponentType());
            if (newCompSection != null) {
                int newChunkIndex = ChunkUtil.indexBlock(newRoot.x, newRoot.y, newRoot.z);
                
                // Fetch the auto-spawned blank placeholder block reference
                Ref<ChunkStore> existingRef = newCompSection.getBlockReference(newChunkIndex);
                if (existingRef != null && existingRef.isValid()) {
                    // Cleanly remove the mapping from the chunk section map
                    newCompSection.removeBlockReference(newChunkIndex, existingRef);
                    // Destroy the temporary placeholder entity from the ECS store to prevent leaks
                    chkStoreStore.removeEntity(existingRef, RemoveReason.REMOVE);
                }
                
                // Map our original stashed block entity reference to the rotated coordinate index
                newCompSection.addBlockReference(newChunkIndex, oldBlockRef);
            }
        }

        state.state = InteractionState.Finished;
        
        //Add sound when editing the block, pulled from CycleBlockGroup interaction
        BlockSoundSet soundSet = BlockSoundSet.getAssetMap().getAsset(targetBlockType.getBlockSoundSetIndex());
        if (soundSet != null) {
            int soundEventIndex = soundSet.getSoundEventIndices().getOrDefault(BlockSoundEvent.Hit, 0);
            if (soundEventIndex != 0) {
                SoundUtil.playSoundEvent3d(playerEntityRef, soundEventIndex, newRoot.x + 0.5D, newRoot.y + 0.5D, newRoot.z + 0.5D, (ComponentAccessor)cmdBuffer);
            }
        } 
    } 
    
    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType intType, @Nonnull InteractionContext intCxt,
            @Nullable ItemStack itmStk, @Nonnull World world, @Nonnull Vector3i blockPos) {
                //Needed to be overridden, but not used in this interaction
            }
}