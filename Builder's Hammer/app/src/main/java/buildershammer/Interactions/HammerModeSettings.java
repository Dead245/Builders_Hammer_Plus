package buildershammer.Interactions;

import java.util.Set;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;

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
        state.state = InteractionState.NotFinished;

        /*
        Uses InteractionVars to store the different settings for each mode. Can be edited via Asset Editor.
        Use key names like "Setting_1", "Setting_2", etc. within InteractionVars
        */
        Set<String> interactionVars = intCxt.getInteractionVars().keySet();

        // Get all "Setting_#" keys in the InteractionVars for the held item. Count them to get the number of settings for the interaction.
        int varCount = (int) interactionVars.stream().filter(key -> key.startsWith("Setting_")).count();

        // How to edit metadata of an item in Hytale
        String settingKey = "Setting";

        Integer setting = heldItem.getFromMetadataOrNull(settingKey, Codec.INTEGER);
        
        // Iterates the 'setting' metadata forwards.
        // withMetadata() returns a copy of the heldItem's ItemStack with updated metadata, so we still need to assign it back to heldItem.
        Integer newSetting = null;
        if (setting == null) {
            newSetting = 1;
        } else if (setting < varCount) {
            newSetting = setting + 1;
        }
        heldItem = heldItem.withMetadata(settingKey, Codec.INTEGER, newSetting);

        intCxt.getHeldItemContainer().setItemStackForSlot(intCxt.getHeldItemSlot(), heldItem);
        intCxt.setHeldItem(heldItem); //Updates interaction context
        
        // Notify player of what the setting is now set to.
        Ref<EntityStore> playerEntityRef = intCxt.getEntity();
        CommandBuffer<EntityStore> cmdBuffer = intCxt.getCommandBuffer();
        
        // Setting names are defined in the interactionVar as well, such as with 'Setting_1-Force Rotate', 'Force Rotate' is the name of the setting
        String settingName;
        if (newSetting == null || varCount == 0) {
            settingName = "Default";
        } else {
            String prefix = settingKey + "_" + newSetting + "-";
            String matchingKey = interactionVars.stream().filter(key -> key.startsWith(prefix)).findFirst().orElse(null);
            if (matchingKey != null){
                settingName = matchingKey.substring(matchingKey.indexOf('-') + 1);
            } else {
                settingName = "Unknown Setting";
            }
        }

        PlayerRef playerRef = cmdBuffer.getComponent(playerEntityRef, PlayerRef.getComponentType());
        PacketHandler playerHandler = playerRef.getPacketHandler();
        var icon = intCxt.getHeldItem().toPacket();

        NotificationUtil.sendNotification(playerHandler, Message.raw(settingName), Message.raw("Hammer setting changed"), null, icon, NotificationStyle.Success, "hammer_setting_change");
        state.state = InteractionState.Finished;
    }
    
}
