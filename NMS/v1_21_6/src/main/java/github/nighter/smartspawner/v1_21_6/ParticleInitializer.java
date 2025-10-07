package github.nighter.smartspawner.v1_21_6;
import github.nighter.smartspawner.nms.ParticleWrapper;
import org.bukkit.Particle;
public class ParticleInitializer {
    public static void init() {
        ParticleWrapper.VILLAGER_HAPPY = Particle.VILLAGER_HAPPY;
        ParticleWrapper.SPELL_WITCH = Particle.SPELL_WITCH;
    }
}