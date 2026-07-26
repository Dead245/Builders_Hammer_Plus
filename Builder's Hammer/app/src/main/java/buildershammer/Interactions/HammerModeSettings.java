package buildershammer.Interactions;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

//Interaction for changing how a mode of the builders hammer works.
public class HammerModeSettings extends SimpleInstantInteraction {
    //Used as one part to register the interaction
    public static final BuilderCodec<HammerModeSettings> CODEC = BuilderCodec.builder(
            HammerModeSettings.class, HammerModeSettings::new, SimpleInstantInteraction.CODEC
    ).build();


    @Override
    protected void firstRun(@Nonnull InteractionType intType, @Nonnull InteractionContext intCxt,
            @Nonnull CooldownHandler cooldownHndlr) {
        ItemStack heldItem = intCxt.getHeldItem();
        
        InteractionSyncData state = intCxt.getState();
        state.state = InteractionState.Failed;

        /*
        Uses InteractionVars to store the different settings for each mode. Can be edited via Asset Editor.
        Use key names like "Setting_1", "Setting_2", etc. within InteractionVars
        */
        
        // Get all "Setting_#" keys in the InteractionVars for the held item. Count them to get the number of settings for the interaction.
        int varCount = (int) intCxt.getInteractionVars().keySet().stream().filter(key -> key.startsWith("Setting_")).count();

        // How to edit metadata of an item in Hytale
        String settingKey = "Setting";

        Integer setting = heldItem.getFromMetadataOrNull(settingKey, Codec.INTEGER);

        // withMetadata() returns a copy of the heldItem's ItemStack with updated metadata, so we still need to assign it back to heldItem.
        if (setting == null) {
            heldItem = heldItem.withMetadata(settingKey, Codec.INTEGER, 1);
        } else if (setting < varCount) {
            heldItem = heldItem.withMetadata(settingKey, Codec.INTEGER, setting + 1);
        } else {
            heldItem = heldItem.withMetadata(settingKey, Codec.INTEGER, null);
        }
        intCxt.getHeldItemContainer().setItemStackForSlot(intCxt.getHeldItemSlot(),heldItem);
        intCxt.setHeldItem(heldItem); //Updates interaction context
        
        state.state = InteractionState.Finished;
    }
    
}
