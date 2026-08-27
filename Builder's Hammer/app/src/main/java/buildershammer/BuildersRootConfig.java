package buildershammer;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;

public class BuildersRootConfig {
    public static final BuilderCodec<BuildersRootConfig> CODEC =
        BuilderCodec.builder(BuildersRootConfig.class, BuildersRootConfig::new)
            .append(new KeyedCodec<>("GlobalRestrictedBlocks", Codec.STRING_ARRAY),
                (config, value) -> config.globalRestrictedBlocks = value,
                config -> config.globalRestrictedBlocks)
                .addValidator(Validators.nonNull())
                .add()
            .append(new KeyedCodec<>("CreativeBypass", Codec.BOOLEAN),
                (config, value) -> config.globalCreativeBypass = value,
                config -> config.globalCreativeBypass)
                .addValidator(Validators.nonNull())
                .add()
            .append(new KeyedCodec<>("Modes", new ArrayCodec<>(BuildersModeConfig.CODEC, BuildersModeConfig[]::new) ),
                (config, value) -> config.modeConfigs = value,
                config -> config.modeConfigs)
                .addValidator(Validators.nonNull())
                .add()
            .build();

    private String[] globalRestrictedBlocks = new String[] {};
    private boolean globalCreativeBypass = true;

    private BuildersModeConfig[] modeConfigs = 
    new BuildersModeConfig[] {
        new BuildersModeConfig("Cycle", new String[] {""}, false),
        new BuildersModeConfig("State", new String[] {"*_Hal*","Plant_Crop*","Soil_Dirt_Tille*"}, false),
        new BuildersModeConfig("Rotate", new String[] { "Teleporter" }, false)
    };

    private final Map<String, BuildersModeConfig> modeCache = new HashMap<>();

    public String[] getRestrictedBlocks(String mode) {
        return Stream.concat(Stream.of(globalRestrictedBlocks), Stream.of(modeCache.get(mode.toLowerCase()).getRestrictedBlocks())).toArray(String[]::new);
    }

    // Makes a map so I can more easily search through the BuildersModeConfig[]
    public void buildCache() {
        modeCache.clear();
        for (BuildersModeConfig mode : this.modeConfigs) {
            modeCache.put(mode.getName().toLowerCase(), mode);
        }
    }

    public boolean isCreativeBypassed(String mode) {
        return globalCreativeBypass || modeCache.get(mode.toLowerCase()).isCreativeBypassed();
    }

}
