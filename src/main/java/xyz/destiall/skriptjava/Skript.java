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

                try {
                    Method load = compiledClass.getDeclaredMethod("load");
                    load.invoke(instance);
                } catch (NoSuchMethodException e) {
                    SkriptJava.getPlugin().getLogger().info("Script " + getCompiledClass() + " does not have a load method!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return instance;
    }
}
