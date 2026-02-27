package buildershammer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocksound.config.BlockSoundSet;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;
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

public class HammerChangeState extends SimpleBlockInteraction {
    
    //Used as one part to register the interaction
    public static final BuilderCodec<HammerChangeState> CODEC = BuilderCodec.builder(
            HammerChangeState.class, HammerChangeState::new, SimpleBlockInteraction.CODEC
    ).build();

    @Override
    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> cmdBuffer,
        @Nonnull InteractionType intType, @Nonnull InteractionContext intCxt, @Nullable ItemStack itmStk,
        @Nonnull Vector3i blockPos, @Nonnull CooldownHandler cooldownHndlr) {
        //BuilderChangeState Interaction
        Ref<EntityStore> userRef = intCxt.getEntity();
        Store<EntityStore> store = userRef.getStore();
        Player playerComponent = cmdBuffer.getComponent(userRef, Player.getComponentType());

        InteractionSyncData interactionState = intCxt.getState();
        interactionState.state = InteractionState.Failed;

        if (playerComponent == null) {
        (HytaleLogger.getLogger().at(Level.INFO)
         .atMostEvery(5, TimeUnit.MINUTES)).log("HammerChangeState requires a Player but was used for: %s", userRef);
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
        
        BlockType blockType = worldChunkComponent.getBlockType(blockPos);

        //Make sure player can change/edit blocks first
        GameplayConfig gameplayConfig = world.getGameplayConfig();
        WorldConfig worldConfig = gameplayConfig.getWorldConfig();

        boolean blockBreakingAllowed = worldConfig.isBlockBreakingAllowed();
        if (!blockBreakingAllowed) return;

        //Get config values
        BuildersHammer bHammer = BuildersHammer.getInstance();
        boolean permission = bHammer.canEdit(blockType.getId(), playerComponent.getGameMode().name(), "");
        if(!permission) return;

        //Remove durability from the held item, pulled from vanilla hammer, doesn't work?
        ItemStack heldItem = intCxt.getHeldItem();
        if (heldItem != null && playerComponent.canDecreaseItemStackDurability(userRef, (ComponentAccessor)store) && !heldItem.isUnbreakable()) {
            playerComponent.updateItemStackDurability(userRef, heldItem, playerComponent.getInventory().getHotbar(), intCxt.getHeldItemSlot(), -heldItem.getItem().getDurabilityLossOnHit(), (ComponentAccessor)cmdBuffer);
        }

        StateData stData = blockType.getState();
        if (stData == null) return;

        //I guess this is the proper way to get the states?
        Map<String, Integer> packetData = stData.toPacket(blockType);

        List<String> states = new ArrayList<>(packetData.keySet());
        states.add(0,"default");

        String currState = blockType.getStateForBlock(blockType); //current state
        int stateIndex = states.indexOf(currState);
        if (stateIndex == -1) stateIndex = 0;

        stateIndex = (stateIndex + 1) % states.size();

        worldChunkComponent.setBlockInteractionState(blockPos, blockType, states.get(stateIndex));

        //Add sound when editing the block, pulled from CycleBlockGroup interaction
        BlockSoundSet soundSet = BlockSoundSet.getAssetMap().getAsset(blockType.getBlockSoundSetIndex());    
        if (soundSet != null) {
            int soundEventIndex = soundSet.getSoundEventIndices().getOrDefault(BlockSoundEvent.Hit, 0);
            if (soundEventIndex != 0) {
                SoundUtil.playSoundEvent3d(userRef, soundEventIndex, blockPos.x + 0.5D, blockPos.y + 0.5D, blockPos.z + 0.5D, (ComponentAccessor)cmdBuffer);
            }
        } 
    }

    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType intType, @Nonnull InteractionContext intCxt,
            @Nullable ItemStack itmStk, @Nonnull World world, @Nonnull Vector3i blockPos) {
                //Needed to be overridden, but not used in this interaction
                //...What is this for?
    }
    
}
