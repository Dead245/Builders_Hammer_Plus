package buildershammer;

import java.lang.reflect.Field;
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
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
//Block states seem to not be fully implemented yet? so this function is not complete until then...
public class HammerChangeState extends SimpleBlockInteraction {
    //Used as one part to register the interaction
    public static final BuilderCodec<HammerChangeState> CODEC = BuilderCodec.builder(
            HammerChangeState.class, HammerChangeState::new, SimpleBlockInteraction.CODEC
    ).build();

    @Override
    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> cmdBuffer,
        @Nonnull InteractionType intType, @Nonnull InteractionContext intCxt, @Nullable ItemStack itmStk,
        @Nonnull Vector3i blockPos, @Nonnull CooldownHandler cooldownHndlr) {
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
        
        //Remove durability from the held item
        ItemStack heldItem = intCxt.getHeldItem();
        if (heldItem != null && playerComponent.canDecreaseItemStackDurability(userRef, (ComponentAccessor)store) && !heldItem.isUnbreakable()) {
            playerComponent.updateItemStackDurability(userRef, heldItem, playerComponent.getInventory().getHotbar(), intCxt.getHeldItemSlot(), -heldItem.getItem().getDurabilityLossOnHit(), (ComponentAccessor)cmdBuffer);
        }
        
        BlockType blockType = worldChunkComponent.getBlockType(blockPos);
        StateData stData = blockType.getState();
        if (stData == null) return;
        
        //This is *very* hacky, 90% chance it will break in the future
        String data = stData.toString(); //This is how I get the definitions lmao

        List<String> states = new ArrayList<>();
        states.add("default");

        int start = data.indexOf('{',data.indexOf("stateToBlock"));
        int end = data.indexOf('}');
        String stateList = data.substring(start+1, end);
        
        String[] keys = stateList.split(",");
        for(String key : keys){
            key = key.trim();
            if(key.isEmpty()) continue;

            int endOfKey = key.indexOf('=');
            if (endOfKey > 0){
                states.add(key.substring(0,endOfKey));
            }
        }
        //Now I have all the state keys in a list :)
        String currState = blockType.getStateForBlock(blockType); //current state
        int stateIndex = 0;
        for (int i = 0; i < states.size(); i++){
            if(states.get(i).equals(currState)){
                stateIndex = i;
            }
        }
        stateIndex++;
        if(stateIndex > states.size()-1){
            stateIndex = 0;
        }

        stData.getBlockForState(states.get(stateIndex));
        worldChunkComponent.setBlockInteractionState(blockPos, blockType, states.get(stateIndex));
    }

    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType intType, @Nonnull InteractionContext intCxt,
            @Nullable ItemStack itmStk, @Nonnull World world, @Nonnull Vector3i blockPos) {
                //Needed to be overridden, but not used in this interaction
    }
    
}
