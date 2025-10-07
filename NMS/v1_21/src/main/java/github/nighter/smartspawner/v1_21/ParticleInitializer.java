package github.nighter.smartspawner.v1_21;

import github.nighter.smartspawner.nms.ParticleWrapper;
import org.bukkit.Particle;

public class ParticleInitializer {
    public static void init() {
        // Tên đúng cho Paper 1.20.1
        ParticleWrapper.VILLAGER_HAPPY = Particle.VILLAGER_HAPPY; 
        ParticleWrapper.SPELL_WITCH = Particle.SPELL_WITCH;
    }
}