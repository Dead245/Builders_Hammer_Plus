package buildershammer;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;

public class BuildersModeConfig
{
    public BuildersModeConfig() {}

    public BuildersModeConfig(String name, String[] modeRestrictedBlocks, boolean creativeBypass) {
        this.modeRestrictedBlocks = modeRestrictedBlocks;
        this.creativeBypass = creativeBypass;
        this.modeName = name;
    }

    public static final BuilderCodec<BuildersModeConfig> CODEC =
        BuilderCodec.builder(BuildersModeConfig.class, BuildersModeConfig::new)
            .append(new KeyedCodec<>("ModeName", Codec.STRING),
                (config, value) -> config.modeName = value,
                config -> config.modeName)
                .addValidator(Validators.nonEmptyString())
                .add()
            .append(new KeyedCodec<>("ModeRestrictedBlocks", Codec.STRING_ARRAY),
                (config, value) -> config.modeRestrictedBlocks = value,
                config -> config.modeRestrictedBlocks).add()
            .append(new KeyedCodec<>("CreativeBypass", Codec.BOOLEAN),
                (config, value) -> config.creativeBypass = value,
                config -> config.creativeBypass)
                .add()
            .build();

    private String modeName;
    private String[] modeRestrictedBlocks = new String[] {""};
    private boolean creativeBypass = false;


    public String getName() {
        return modeName;
    }

    public boolean isCreativeBypassed() {
        return creativeBypass;
    }

    public String[] getRestrictedBlocks() {
        return modeRestrictedBlocks;
    }
}