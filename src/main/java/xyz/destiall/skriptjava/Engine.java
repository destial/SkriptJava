package xyz.destiall.skriptjava;

import org.bukkit.Bukkit;

import javax.script.ScriptException;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Engine {
    private final Map<String, Class<?>> classes = new ConcurrentHashMap<>();
    private final Map<String, ScriptClassLoader> loaders = new ConcurrentHashMap<>();
    private final JavaCompiler compiler;

    private ScriptMemoryManager scriptMemoryManager;
    private DiagnosticCollector<JavaFileObject> diagnostics;
    private List<String> compileOptions;

    public Engine() {
        compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return;
        }
        diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(diagnostics, null, null);
        scriptMemoryManager = new ScriptMemoryManager(standardFileManager, getClass().getClassLoader());
        reloadLibraries();
    }

    public void reloadLibraries() {
        File plugins = new File(System.getProperty("user.dir"), "plugins" + File.separator);
        List<File> dependencies = FileIO.traverse(plugins, f -> f.getName().endsWith(".jar"));

        File library = new File(System.getProperty("user.dir"), "libraries" + File.separator);
        if (library.exists()) {
            SkriptJava.getPlugin().getLogger().info("Adding libraries to class path");
            dependencies.addAll(FileIO.traverse(library, f -> f.getName().endsWith(".jar")));
        } else {
            File source = new File(Bukkit.class.getProtectionDomain().getCodeSource().getLocation().getPath());
            dependencies.add(source);
        }

        URI uri = new File("").toURI();
        String classPath = dependencies.stream().map(f -> "./" + uri.relativize(f.toURI()).getPath()).collect(Collectors.joining(";"));
        compileOptions = Arrays.asList("-cp", classPath);
    }

    public Set<String> getNames() {
        return classes.keySet();
    }

    public Skript get(String name) {
        Map.Entry<String, ScriptClassLoader> entry = loaders.entrySet().stream().filter(en -> en.getKey().endsWith(name)).findFirst().orElse(null);
        if (entry == null) return null;
        return entry.getValue().script;
    }

    public Skript getByClass(Class<?> clazz) {
        return get(clazz.getName());
    }

    public Skript removeScript(String name) {
        Map.Entry<String, ScriptClassLoader> entry = loaders.entrySet().stream().filter(en -> en.getKey().endsWith(name)).findFirst().orElse(null);
        if (entry == null) return null;
        loaders.remove(entry.getKey());
        classes.remove(entry.getKey());
        try {
            entry.getValue().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return entry.getValue().script;
    }

    public void removeScript(Skript skript) {
        Map.Entry<String, ScriptClassLoader> entry = loaders.entrySet().stream().filter(en -> en.getValue().script == skript).findFirst().orElse(null);
        if (entry == null) return;
        loaders.remove(entry.getKey());
        classes.remove(entry.getKey());
        try {
            entry.getValue().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Class<?> getClassByName(final String name) {
        Class<?> cachedClass = classes.get(name);

        if (cachedClass != null) {
            return cachedClass;
        } else {
            for (String current : loaders.keySet()) {
                ScriptClassLoader loader = loaders.get(current);
                try {
                    cachedClass = loader.findClass(name);
                } catch (ClassNotFoundException ignored) {}
                if (cachedClass != null) {
                    return cachedClass;
                }
            }
        }
        return null;
    }

    Class<?> getClass(String name) {
        return classes.get(name);
    }

    void setClass(final String name, final Class<?> clazz) {
        if (!classes.containsKey(name)) {
            classes.put(name, clazz);
        }
    }

    boolean removeClass(String name) {
        return classes.remove(name) != null;
    }

    public Skript compile(File file) throws ScriptException, MalformedURLException {
        return compile(file, FileIO.readData(file));
    }

    public Skript compile(File file, String code) throws ScriptException, MalformedURLException {
        if (compiler == null) {
            throw new NullPointerException("You are not running on a compatible version of the Java Development Kit! You cannot use scripts!");
        }

        String fullClassName = FileIO.getFullName(file, code);

        FileScriptMemoryJavaObject scriptSource = scriptMemoryManager.createSourceFileObject(file, fullClassName, code);
        Collection<FileScriptMemoryJavaObject> otherScripts = loaders.values().stream().filter(l -> !l.getFullClassName().equals(fullClassName)).map(ScriptClassLoader::getSource).collect(Collectors.toList());
        otherScripts.add(scriptSource);

        JavaCompiler.CompilationTask task = compiler.getTask(null, scriptMemoryManager, diagnostics, compileOptions, null, otherScripts);
        if (!task.call()) {
            String message = "Error while compiling " + file.getPath() + ": " + diagnostics.getDiagnostics().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining("\n"));
            throw new ScriptException(message);
        }

        for (Map.Entry<String, ScriptClassLoader> entry : loaders.entrySet()) {
            ScriptClassLoader previous = entry.getValue();
            removeClass(previous.getFullClassName());
            ScriptClassLoader newLoader = scriptMemoryManager.getClassLoader(this, previous.getFile(), previous.getFullClassName(), previous.getSource());
            entry.setValue(newLoader);
            setClass(previous.getFullClassName(), newLoader.script.getCompiledClass());
            try {
                previous.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ScriptClassLoader previous = loaders.remove(fullClassName);
        ScriptClassLoader loader = scriptMemoryManager.getClassLoader(this, file, fullClassName, scriptSource);
        if (previous != null) {
            removeClass(previous.getFullClassName());
            try {
                previous.close();
                previous.clearAssertionStatus();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        loaders.put(fullClassName, loader);
        setClass(fullClassName, loader.script.getCompiledClass());
        return loader.script;
    }

    public List<Skript> compileAll(List<File> files) throws ScriptException {
        if (compiler == null) {
            throw new NullPointerException("You are not running on a compatible version of the Java Development Kit! You cannot use scripts!");
        }
        List<FileScriptMemoryJavaObject> sources = new ArrayList<>(files.size());
        for (File file : files) {
            if (!file.exists()) continue;
            String code = FileIO.readData(file);
            String fullClassName = FileIO.getFullName(file, code);
            FileScriptMemoryJavaObject object = scriptMemoryManager.createSourceFileObject(file, fullClassName, code);
            sources.add(object);
        }

        JavaCompiler.CompilationTask task = compiler.getTask(null, scriptMemoryManager, diagnostics, compileOptions, null, sources);
        if (!task.call()) {
            String message = "Error while compiling sources: " + diagnostics.getDiagnostics().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining("\n"));
            throw new ScriptException(message);
        }

        for (FileScriptMemoryJavaObject source : sources) {
            ScriptClassLoader previous = loaders.get(source.getName());
            removeClass(source.getName());
            ScriptClassLoader loader = null;
            try {
                loader = scriptMemoryManager.getClassLoader(this, source.getOrigin(), source.getName(), source);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            if (loader == null) continue;
            removeClass(source.getName());
            if (previous != null) {
                try {
                    previous.close();
                    previous.clearAssertionStatus();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            loaders.put(source.getName(), loader);
            setClass(source.getName(), loader.script.getCompiledClass());
        }

        return loaders.values().stream().map(l -> l.script).collect(Collectors.toList());
    }

    public void removeAll() {
        for (ScriptClassLoader loader : loaders.values()) {
            try {
                 loader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        loaders.clear();
        classes.clear();
    }
}
