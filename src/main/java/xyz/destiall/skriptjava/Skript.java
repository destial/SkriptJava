package xyz.destiall.skriptjava;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class Skript {
    private final Class<?> compiledClass;
    private final CharSequence code;
    private final File file;
    private Object instance;
    private boolean loaded = false;
    private boolean unloaded = false;

    public Skript(Class<?> compiledClass, File file, CharSequence code) {
        this.compiledClass = compiledClass;
        this.code = code;
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public CharSequence getCode() {
        return code;
    }

    public Class<?> getCompiledClass() {
        return compiledClass;
    }

    public void load() {
        if (loaded && !unloaded) return;
        try {
            Method load = compiledClass.getDeclaredMethod("load");
            load.setAccessible(true);
            load.invoke(instance);
            loaded = true;
            unloaded = false;
        } catch (Exception e) {
            SkriptJava.getPlugin().getLogger().info("Script " + getCompiledClass() + " does not have a load method!");
        }
    }

    public void unload() {
        if (unloaded && !loaded) return;
        try {
            Method unload = compiledClass.getDeclaredMethod("unload");
            unload.setAccessible(true);
            unload.invoke(instance);
            unloaded = true;
            loaded = false;
        } catch (Exception e) {
            SkriptJava.getPlugin().getLogger().info("Script " + getCompiledClass() + " does not have an unload method!");
        }
    }

    public Object getInstance() {
        if (instance == null) {
            try {
                try {
                    Constructor<?> constructor = compiledClass.getConstructor(Plugin.class);
                    constructor.setAccessible(true);
                    instance = constructor.newInstance(SkriptJava.getPlugin());
                } catch (NoSuchMethodException e) {
                    SkriptJava.getPlugin().getLogger().info("No plugin constructor! Using default constructor...");
                    Constructor<?> constructor = compiledClass.getConstructor();
                    constructor.setAccessible(true);
                    instance = constructor.newInstance();
                }

                load();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return instance;
    }
}
