package buildershammer;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class BuildersRootConfig {
    public static final BuilderCodec<BuildersRootConfig> CODEC =
        BuilderCodec.builder(BuildersRootConfig.class, BuildersRootConfig::new)
            .append(new KeyedCodec<>("GlobalRestrictedBlocks", BuilderCodec.STRING_ARRAY),
                (config, value) -> config.globalRestrictedBlocks = value,
                config -> config.globalRestrictedBlocks).add()
            .append(new KeyedCodec<>("CreativeBypass", BuilderCodec.BOOLEAN),
                (config, value) -> config.creativeBypass = value,
                config -> config.creativeBypass).add()
            .build();

    private String[] globalRestrictedBlocks = new String[] {"*_Hal*","Plant_Crop*","Soil_Dirt_Tille*"};
    private boolean creativeBypass = true;

    public String[] getGlobalRestrictedBlocks() {
        return globalRestrictedBlocks;
    }

    public boolean isCreativeBypassed() {
        return creativeBypass;
    }
}