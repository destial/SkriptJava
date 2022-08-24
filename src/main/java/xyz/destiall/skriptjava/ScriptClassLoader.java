package xyz.destiall.skriptjava;

import javax.script.ScriptException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Map;

public final class ScriptClassLoader extends URLClassLoader {
    private final Engine loader;
    public static final String MEMORY_CLASS_URL = "http://ch.obermuhlner/ch.obermuhlner.scriptengine.java/memory-class";

    private final ProtectionDomain protectionDomain;
    private final Map<String, ClassScriptMemoryJavaObject> mapClassBytes;
    private final File file;
    public final Skript script;
    private final FileScriptMemoryJavaObject source;
    private final String fullClassName;

    ScriptClassLoader(Engine loader, Map<String, ClassScriptMemoryJavaObject> mapNameToBytes, ClassLoader parent, File file, String fullClassName, FileScriptMemoryJavaObject source) throws ScriptException, MalformedURLException {
        super(new URL[] {file.toURI().toURL()}, parent);
        this.mapClassBytes = mapNameToBytes;
        try {
            URL url = new URL(MEMORY_CLASS_URL);
            CodeSource codeSource = new CodeSource(url, (Certificate[]) null);
            protectionDomain = new ProtectionDomain(codeSource, null, this, new Principal[0]);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        this.loader = loader;
        this.file = file;
        this.source = source;
        this.fullClassName = fullClassName;
        try {
            Class<?> clazz = loadClass(fullClassName);
            this.script = new Skript(clazz, file, this.source.getCharContent(false));
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    public String getFullClassName() {
        return fullClassName;
    }

    public FileScriptMemoryJavaObject getSource() {
        return source;
    }

    public File getFile() {
        return file;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> result = loader.getClassByName(name);
        if (result == null) {
            result = super.findClass(name);
            if (result != null) {
                loader.setClass(name, result);
            }
        }
        return result;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> find = loader.getClass(name);
        if (find != null) {
            return find;
        }
        ClassScriptMemoryJavaObject object = mapClassBytes.get(name);
        if (object == null) {
            return super.loadClass(name);
        }
        byte[] bytes = object.getBytes();
        return defineClass(name, bytes, 0, bytes.length, protectionDomain);
    }
}
