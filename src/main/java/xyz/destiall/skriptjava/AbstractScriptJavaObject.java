package xyz.destiall.skriptjava;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.net.URI;

public abstract class AbstractScriptJavaObject extends SimpleJavaFileObject {
    public AbstractScriptJavaObject(String name, Kind kind) {
        super(URI.create("memory:///" + name.replace('.', '/') + kind.extension), kind);
    }
}
