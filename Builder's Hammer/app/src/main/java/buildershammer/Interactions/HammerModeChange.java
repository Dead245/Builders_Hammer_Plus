package buildershammer.Interactions;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

import buildershammer.BuildersHammer;
import buildershammer.Helpers.ModeList;

// Interaction that cycles the hammer through a set of states, which represent different modes. The states are defined in ModeList.java. The default mode is "Cycle", which is just the item without a state.
public class HammerModeChange extends SimpleInstantInteraction{
    //Used as one part to register the interaction
    public static final BuilderCodec<HammerModeChange> CODEC = BuilderCodec.builder(
            HammerModeChange.class, HammerModeChange::new, SimpleInstantInteraction.CODEC
    ).build();

    @Override
    protected void firstRun(@Nonnull InteractionType intType, @Nonnull InteractionContext intContext,
            @Nonnull CooldownHandler cooldownHndlr) {
        String heldItemId = intContext.getHeldItem().getItemId();
        Item itemAsset = Item.getAssetMap().getAsset(heldItemId);
        if (itemAsset == null) {
            BuildersHammer.LOGGER.atInfo().log("HammerModeChange could not find item ID for: %s", heldItemId);
            intContext.getState().state = InteractionState.Failed;
            return;
        }

        //Check if default, I think
        if (!intContext.getHeldItem().getItem().isState()) {
            BuildersHammer.LOGGER.atInfo().log("HammerModeChange - Held item is in default mode");
        }

        for (int i = 0; i < ModeList.modes.length; i++) {
            String mode = ModeList.modes[i];
            if (heldItemId.equals(intContext.getHeldItem().withState(mode).getItemId())) {
                int nextIndex = (i + 1) % ModeList.modes.length;
                
                //withState() will throw an IllegalArgumentException if the item doesn't have that state
                ItemStack newItem = intContext.getHeldItem().withState(ModeList.modes[nextIndex]);
                //TODO transfer things to the new item, like durability and certain metadata like the saved block info.

                //Remove old item, is this needed before setting it to a new item?
                intContext.getHeldItemContainer().removeItemStackFromSlot(intContext.getHeldItemSlot(),intContext.getHeldItem(),1);
                //Add new item
                intContext.getHeldItemContainer().setItemStackForSlot(intContext.getHeldItemSlot(),newItem);
                intContext.setHeldItem(newItem); //Updates interaction context

                intContext.getState().state = InteractionState.Finished;
                return;
            }
        }
        
        BuildersHammer.LOGGER.atWarning().log("HammerModeChange could not find matching mode for held item ID: %s", heldItemId);
        intContext.getState().state = InteractionState.Failed;
    }

}
    
 