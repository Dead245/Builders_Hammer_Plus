package buildershammer.Interactions;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3i;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.meta.DynamicMetaStore;
import com.hypixel.hytale.server.core.meta.MetaKey;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.operation.OperationsBuilder;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import buildershammer.Helpers.PositionHelpers;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

// Gets blocks in an AOE around the targetted block with a configurable size, and runs an interaction on each block.
// Based loosely on the vanilla RunOnBlockTypesInteraction.java
// This interaction can honestly be done in a SimpleInteraction instead
public class AOEBlockSelection extends SimpleBlockInteraction {

    //Used as one part to register the interaction
    public static final BuilderCodec<AOEBlockSelection> CODEC = BuilderCodec.builder(AOEBlockSelection.class, AOEBlockSelection::new, SimpleBlockInteraction.CODEC)
        .append(new KeyedCodec<>("Size", BuilderCodec.INTEGER),
            (config, value) -> config.size = value,
            config -> config.size)
        .documentation("The radius of the AOE in blocks.")
        .addValidator(Validators.greaterThan(Integer.valueOf(0)))
        .add()
        .append(new KeyedCodec<>("Delay", BuilderCodec.LONG),
                (config, value) -> config.delay = value,
                config -> config.delay)
        .documentation("[Not Implemented] The delay in seconds between each AOE ring.")
        .add()
        .append(new KeyedCodec<>("Interaction", RootInteraction.CHILD_ASSET_CODEC, true),
            (config, value) -> config.interaction = value,
            config -> config.interaction)
        .documentation("The interaction to run on each found block. Can be defined inline or as a reference.")
        .addValidatorLate(() -> RootInteraction.VALIDATOR_CACHE.getValidator().late())
        .add() 
        .append(new KeyedCodec<>("Filter Blocks", BuilderCodec.BOOLEAN), 
            (config, value) -> config.filterBlocks = value,
            config -> config.filterBlocks) // Ideally have the player choose, but for the first version, have it as an option in the interaction.
        .documentation("If the AOE should filter what is affected based on the block originally targetted.")
        .add()
        .build();

    // Adding custom data to the interaction (from above)
    protected int size = 1;
    protected long delay;
    protected String interaction;
    protected boolean filterBlocks;
    
    enum Shape{
        CUBE,
        SPHERE,
        PLANE
    }

    // This is how we store data between ticks while the interaction runs over time
    private static final MetaKey<ObjectArrayList<InteractionChain>> FORKED_CHAINS = META_REGISTRY.registerMetaObject();
    private static final MetaKey<Boolean> ANY_SUCCEEDED = META_REGISTRY.registerMetaObject();
    private static final MetaKey<Int2ObjectSortedMap<List<Vector3i>>> RINGS = META_REGISTRY.registerMetaObject();;
    private static final MetaKey<Integer> CURRENT_RING = META_REGISTRY.registerMetaObject();


    @Override
    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> cmdBuffer,
            @Nonnull InteractionType intType, @Nonnull InteractionContext intCxt, @Nullable ItemStack itemStk,
            @Nonnull Vector3i blockPos, @Nonnull CooldownHandler cooldownHdlr) {
        // Called once at the start, basically the first run of the interaction.
        // For each tick after this interaction is NotFinished, it calls tick0().
        DynamicMetaStore<Interaction> instanceStore = intCxt.getInstanceStore();
        
        // Calculate the positions and potential rings of the AOE, then set to NotFinished

        // TODO - implement different shapes for the positions
        List<Vector3i> positions;
        if (filterBlocks) {
            positions = PositionHelpers.generateCubePositions(blockPos, this.size, world,  IntSets.singleton(world.getBlock(blockPos)));

        } else {
            positions = PositionHelpers.generateCubePositions(blockPos, this.size, world, null);
        }

        // Get the rings to ripple
        Int2ObjectSortedMap<List<Vector3i>> rings = PositionHelpers.radialPositionSort(positions, blockPos);
        if (rings == null || rings.isEmpty()){
            intCxt.getState().state = InteractionState.Failed;
            return;
        }

        instanceStore.putMetaObject(ANY_SUCCEEDED, false);
        instanceStore.putMetaObject(RINGS,rings);
        instanceStore.putMetaObject(CURRENT_RING, 0);

        // Start the first set of blocks
        initiateInteractions(world,intCxt,rings.get(0));

        intCxt.getState().state = InteractionState.NotFinished;
    }

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type, @Nonnull InteractionContext intCxt, @Nonnull CooldownHandler cooldownHandler) {
        if (firstRun) {
        super.tick0(firstRun,time,type,intCxt,cooldownHandler);
        return;
        }

        // Continuation point for the interaction, since interactWithBlock() is only for the first run of it.
        // We set the interactionState to NotFinished in interactWithBlock() to end up here:
        World world = intCxt.getCommandBuffer().getExternalData().getWorld();
        
        var instanceStore = intCxt.getInstanceStore();

        var chains = instanceStore.getMetaObject(FORKED_CHAINS);
        if (chains == null) {
            intCxt.getState().state =
            InteractionState.Failed;
            return;
        }


        boolean allFinished = true;
        // Instance Store object might not exist yet and be null, so we check it here
        boolean anySucceeded = Boolean.TRUE.equals(instanceStore.getMetaObject(ANY_SUCCEEDED));

        for (var chain : chains) {
            switch(chain.getServerState()) {
                case NotFinished:
                    allFinished = false;
                    break;
                case Finished:
                    anySucceeded = true;
                    break;
                case Failed:
                    break;
                case ItemChanged:
                    break;
                case Skip:
                    break;
                default:
                    break;
            }
        }

        instanceStore.putMetaObject(ANY_SUCCEEDED,anySucceeded);
        
        if (!allFinished) {
            // Not all of the current running ring of interactions is done, so return for now and try again
            intCxt.getState().state =
            InteractionState.NotFinished;
            return;
        }

        // Ring finished
        var rings = instanceStore.getMetaObject(RINGS);
        int currentRing = instanceStore.getMetaObject(CURRENT_RING);

        currentRing++;

        if (currentRing < rings.size()) {
            // Start the next ring of interactions if there IS a next ring to do
            instanceStore.putMetaObject(CURRENT_RING,currentRing);
            world = intCxt.getCommandBuffer().getExternalData().getWorld();
            initiateInteractions(world, intCxt, rings.get(currentRing));

            intCxt.getState().state = InteractionState.NotFinished;
            return;
        }

        // Finished all rings
        intCxt.getState().state = anySucceeded ? InteractionState.Finished : InteractionState.Failed;

        super.tick0(firstRun, time, type, intCxt, cooldownHandler);
    }

    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType arg0, @Nonnull InteractionContext arg1,
            @Nullable ItemStack arg2, @Nonnull World arg3, @Nonnull Vector3i arg4) {}



    public void initiateInteractions(World world, InteractionContext intCxt, List<Vector3i> ring){
        var instanceStore = intCxt.getInstanceStore();

        if (ring == null) return;

        RootInteraction rootInteraction = RootInteraction.getAssetMap().getAsset(interaction);
        if(rootInteraction == null) return;
        

        var chains = new ObjectArrayList<InteractionChain>(ring.size());
        for(Vector3i pos : ring) {

            InteractionContext forkedContext = intCxt.duplicate();

            BlockPosition target = new BlockPosition(pos.x,pos.y,pos.z);
            forkedContext.getMetaStore().putMetaObject(Interaction.TARGET_BLOCK_RAW,target);

            forkedContext.getMetaStore().putMetaObject(Interaction.TARGET_BLOCK, target);

            InteractionChain chain = intCxt.fork(forkedContext, rootInteraction, false);
            
            chains.add(chain);
        }

        instanceStore.putMetaObject(FORKED_CHAINS, chains);
    }

    // Pulled straight from RunOnBlockTypesInteraction.java, then replaced the deprecated functions
    @Override
    public void compile(@Nonnull OperationsBuilder builder) {
        // This interaction handles child interactions via forking at runtime,
        // so we only add ourselves to the operation list.
        // Next/Failed are handled by SimpleInteraction.
        if (next == null && failed == null) {
            builder.addOperation(this);
            return;
        }

        var failedLabel = builder.createUnresolvedLabel();
        var endLabel = builder.createUnresolvedLabel();

        builder.addOperation(this, failedLabel);

        // Next path
        if (next != null) {
            var nextInteraction = Interaction.getAssetMap().getAsset(next);
            nextInteraction.compile(builder);
        }

        if (failed != null) builder.jump(endLabel);

        // Failed path
        builder.resolveLabel(failedLabel);
        if (failed != null) {
            var failedInteraction = Interaction.getAssetMap().getAsset(failed);
            failedInteraction.compile(builder);
        }

        builder.resolveLabel(endLabel);
    }
    
}
