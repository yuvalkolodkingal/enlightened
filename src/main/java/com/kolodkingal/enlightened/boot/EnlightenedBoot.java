package com.kolodkingal.enlightened.boot;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import java.lang.reflect.InvocationTargetException;

/**
 * The mod's only entry point: finds the copy of the mod for the running Minecraft version ({@link Versions}) and
 * starts it, the common half and, on a client, the client half. Nothing here may use an API that differs between the
 * supported versions; the copies are reached by name only, so the others are never loaded.
 */
@Mod("enlightened")
public final class EnlightenedBoot {
    public EnlightenedBoot(IEventBus modBus, ModContainer container, Dist dist) {
        String pkg = Versions.packageForRunning();
        if (pkg == null) {
            throw new IllegalStateException("Enlightened " + container.getModInfo().getVersion() + " supports Minecraft "
                    + Versions.supported() + ", not " + Versions.minecraft());
        }
        start(pkg + ".Enlightened", new Class<?>[] {IEventBus.class, ModContainer.class}, modBus, container);
        if (dist.isClient()) {
            start(pkg + ".client.EnlightenedClient", new Class<?>[] {ModContainer.class, IEventBus.class}, container, modBus);
        }
    }

    private static void start(String className, Class<?>[] types, Object... args) {
        try {
            Class.forName(className).getDeclaredConstructor(types).newInstance(args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Enlightened could not start " + className, cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Enlightened could not start " + className, e);
        }
    }
}
