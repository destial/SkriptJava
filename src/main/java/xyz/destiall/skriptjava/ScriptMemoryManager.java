package xyz.destiall.skriptjava;

import javax.script.ScriptException;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static javax.tools.StandardLocation.CLASS_OUTPUT;

public final class ScriptMemoryManager extends ForwardingJavaFileManager<JavaFileManager> {

    private final Map<String, ClassScriptMemoryJavaObject> mapNameToClasses = new ConcurrentHashMap<>();
    private final ClassLoader parentClassLoader;

    public ScriptMemoryManager(JavaFileManager fileManager, ClassLoader parentClassLoader) {
        super(fileManager);

        this.parentClassLoader = parentClassLoader;
    }

    private Collection<ClassScriptMemoryJavaObject> memoryClasses() {
        return mapNameToClasses.values();
    }

    public FileScriptMemoryJavaObject createSourceFileObject(File origin, String name, String code) {
        return new FileScriptMemoryJavaObject(origin, name, JavaFileObject.Kind.SOURCE, code);
    }

    public ScriptClassLoader getClassLoader(Engine loader, File file, String fullClassName, FileScriptMemoryJavaObject source) throws ScriptException, MalformedURLException {
        return new ScriptClassLoader(loader, mapNameToClasses, parentClassLoader, file, fullClassName, source);
    }

    @Override
    public Iterable<JavaFileObject> list(
            JavaFileManager.Location location,
            String packageName,
            Set<JavaFileObject.Kind> kinds,
            boolean recurse) throws IOException {
        Iterable<JavaFileObject> list = super.list(location, packageName, kinds, recurse);

        if (location == CLASS_OUTPUT) {
            Collection<? extends JavaFileObject> generatedClasses = memoryClasses();
            return () -> new CompositeIterator<>(
                    list.iterator(),
                    generatedClasses.iterator());
        }

        return list;
    }

    @Override
    public String inferBinaryName(JavaFileManager.Location location, JavaFileObject file) {
        if (file instanceof ClassScriptMemoryJavaObject) {
            return file.getName();
        } else {
            return super.inferBinaryName(location, file);
        }
    }

    @Override
    public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        if (kind == JavaFileObject.Kind.CLASS) {
            ClassScriptMemoryJavaObject file = new ClassScriptMemoryJavaObject(className);
            mapNameToClasses.put(className, file);
            return file;
        }

        return super.getJavaFileForOutput(location, className, kind, sibling);
    }
}
