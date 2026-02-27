package buildershammer;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;

public class BuildersHammer extends JavaPlugin{
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final Config<BuildersRootConfig> config;
    private static BuildersHammer instance;

    public BuildersHammer(JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log("Plugin %s version %s initialized.", this.getName(),this.getManifest().getVersion().toString());
        this.config = this.withConfig("BuildersHammerConfig", BuildersRootConfig.CODEC);
    }

    @Override
    protected void setup() {
        this.config.save();

        this.getCodecRegistry(Interaction.CODEC).register("BuilderRotateY", HammerRotationY.class, HammerRotationY.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("BuilderRotateXZ", HammerRotationXZ.class, HammerRotationXZ.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("BuilderChangeState", HammerChangeState.class, HammerChangeState.CODEC);
    }

    public boolean canEdit(String blockID, String gamemode, String action){
        BuildersRootConfig rootConfig = config.get();
        if (gamemode.equals("Creative") && rootConfig.isCreativeBypassed()) return true;

        if (blockID.startsWith("*")) blockID = blockID.substring(1);
        
        blockID = blockID.trim();
        for (String id : rootConfig.getGlobalRestrictedBlocks()) {
            String regex = id.trim().replace("*", ".*");

            if (blockID.matches(regex)) {
                return false;
            }
        }
        return true;
    }

    public static BuildersHammer getInstance() {
        return instance;
    }
}