package github.nighter.smartspawner.v1_21_4;

import github.nighter.smartspawner.nms.TextureWrapper;
import org.bukkit.entity.EntityType;

/**
 * This class is responsible for initializing version-specific textures
 * for various entity types in the SmartSpawner plugin.
 */
public class TextureInitializer {

    /**
     * Initializes version-specific textures for various entity types.
     * This method should be called during the plugin's initialization phase.
     */
    public static void init() {
        TextureWrapper.addVersionSpecificTexture(
                EntityType.ARMADILLO,
                "9164ed0e0ef69b0ce7815e4300b4413a4828fcb0092918543545a418a48e0c3c"
        );
        TextureWrapper.addVersionSpecificTexture(
                EntityType.BOGGED,
                "a3b9003ba2d05562c75119b8a62185c67130e9282f7acbac4bc2824c21eb95d9"
        );
        TextureWrapper.addVersionSpecificTexture(
                EntityType.BREEZE,
                "a275728af7e6a29c88125b675a39d88ae9919bb61fdc200337fed6ab0c49d65c"
        );
        TextureWrapper.addVersionSpecificTexture(
                EntityType.MOOSHROOM,
                "45603d539f666fdf0f7a0fe20b81dfef3abe6c51da34b9525a5348432c5523b2"
        );
        TextureWrapper.addVersionSpecificTexture(
                EntityType.SNOW_GOLEM,
                "1fdfd1f7538c040258be7a91446da89ed845cc5ef728eb5e690543378fcf4"
        );
        TextureWrapper.addVersionSpecificTexture(
                EntityType.CREAKING,
                "ac91c87bbe7f4c586e0f8b60f9b76d173a41daa302944531703be9ff4fd117f8"
        );
    }
}