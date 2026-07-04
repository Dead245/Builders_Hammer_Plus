package buildershammer.Interactions;

import java.util.Map;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

import buildershammer.BuildersHammer;

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

        // Use InteractionVars to store the different settings. Can be edited via Asset Editor.
        Map<String, String> interactionVars = intCxt.getInteractionVars();

        // Get current setting and then set it to the next one in the  InteractionVar list.
        // If the current setting is not found, it will default to the first setting in the list.
        BuildersHammer.LOGGER.atInfo().log("--- HammerModeSettings Triggered! ---");
        state.state = InteractionState.Finished;
    }
    
}
