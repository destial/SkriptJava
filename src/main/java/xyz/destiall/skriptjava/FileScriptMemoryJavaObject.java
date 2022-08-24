package xyz.destiall.skriptjava;

import java.io.File;

public final class FileScriptMemoryJavaObject extends AbstractScriptJavaObject {
    private final File origin;
    private final String code;
    private final String className;

    FileScriptMemoryJavaObject(File origin, String className, Kind kind, String code) {
        super(className, kind);
        this.className = className;
        this.origin = origin;
        this.code = code;
    }

    @Override
    public String getName() {
        return className;
    }

    public File getOrigin() {
        return origin;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }

    public String getCode() {
        return code;
    }
}
