package buildershammer;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class BuildersHammer extends JavaPlugin{
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    public BuildersHammer(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Plugin %s version %s initialized.", this.getName(),this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        this.getCodecRegistry(Interaction.CODEC).register("BuilderRotateY", HammerRotationY.class, HammerRotationY.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("BuilderRotateXZ", HammerRotationXZ.class, HammerRotationXZ.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("BuilderChangeState", HammerChangeState.class, HammerChangeState.CODEC);
    }
}