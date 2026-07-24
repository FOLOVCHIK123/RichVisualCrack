package rich.modules.module;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import rich.modules.impl.combat.*;
import rich.modules.impl.combat.NoInteract;
import rich.modules.impl.combat.AutoTotem;
import rich.modules.impl.misc.*;
import rich.modules.impl.misc.autoparser.AutoParser;
import rich.modules.impl.movement.*;
import rich.modules.impl.player.*;
import rich.modules.impl.render.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleRepository {
    List<ModuleStructure> moduleStructures = new ArrayList<>();
    List<ModuleStructure> hiddenModules = new ArrayList<>();
    Set<Class<? extends ModuleStructure>> registeredClasses = new HashSet<>();

    public void setup() {
        builder()
                .add(new Hud())
                .add(new HitEffect())
                .add(new WorldParticles())
                .add(new Particles())
                .add(new GlassHands())
                .add(new ChunkAnimator())
                .add(new Ambience())
                .add(new ChinaHat())
                .add(new ClientSounds())
                .add(new TargetESP())
                .add(new BlockOverlay())
                .add(new NoRender())
                .add(new HitSound())
                .add(new JumpCircle())
                .add(new ClickFriend())
                .add(new FreeLook())
                .add(new FullBright())
                .add(new ItemPhysic())
                .add(new NameProtect())
                .add(new NoFriendDamage())
                .add(new ViewModel())
                .add(new AutoRespawn())
                .add(new SwingAnimation())
                .add(new Esp())
                .add(new NoInteract())
                .add(new AutoSprint());

    }

    public ModuleBuilder builder() {
        return new ModuleBuilder(this);
    }

    void registerModule(ModuleStructure module, boolean hidden) {
        Class<? extends ModuleStructure> clazz = module.getClass();
        if (registeredClasses.contains(clazz)) {
            throw new DuplicateModuleException(clazz.getSimpleName());
        }
        registeredClasses.add(clazz);
        if (hidden) {
            hiddenModules.add(module);
            module.setState(true);
        } else {
            moduleStructures.add(module);
        }
    }

    public List<ModuleStructure> modules() {
        return moduleStructures;
    }

    public List<ModuleStructure> hiddenModules() {
        return hiddenModules;
    }

    public List<ModuleStructure> allModules() {
        List<ModuleStructure> all = new ArrayList<>(moduleStructures);
        all.addAll(hiddenModules);
        return all;
    }
}