package nl.elec332.gradle.minecraft.moddev.tasks;

import nl.elec332.gradle.minecraft.moddev.ModLoader;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.bundling.Zip;

/**
 * Created by Elec332 on 19-03-2024
 */
public abstract class RemapJarTask extends Zip {

    public RemapJarTask() {
        super.setEnabled(false);
    }

    private boolean setup = false;

    public void setup(AbstractArchiveTask root) {
        if (setup) {
            throw new IllegalStateException();
        }
        setup = true;
        dependsOn(root);
        getInputs().file(root.getOutputs().getFiles());
        getOutputs().file(root.getOutputs().getFiles());
        getDestinationDirectory().convention(root.getDestinationDirectory());
        ProjectHelper.copyNameTo(root, this);
    }

    @Input
    public abstract Property<ModLoader.Mapping> getMapping();

    @Override
    public void setEnabled(boolean enabled) {
        throw new UnsupportedOperationException();
    }

}
