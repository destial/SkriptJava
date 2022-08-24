package xyz.destiall.skriptjava;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FileIO {
    private FileIO() {}
    private static final Pattern NAME_PATTERN = Pattern.compile("public\\s+class\\s+([A-Za-z][A-Za-z0-9_$]*)");
    private static final Pattern NAME_FINAL_PATTERN = Pattern.compile("public\\s+final\\s+class\\s+([A-Za-z][A-Za-z0-9_$]*)");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([A-Za-z][A-Za-z0-9_$.]*)");

    public static String getPackage(String script) {
        String fullPackage = null;
        Matcher packageMatcher = PACKAGE_PATTERN.matcher(script);
        if (packageMatcher.find()) {
            fullPackage = packageMatcher.group(1);
        }
        return fullPackage;
    }

    public static String getFullName(File file, String script) {
        String fullPackage = getPackage(script);

        Matcher nameMatcher = NAME_PATTERN.matcher(script);
        if (nameMatcher.find()) {
            String name = nameMatcher.group(1);
            if (fullPackage == null) {
                return name;
            } else {
                return fullPackage + "." + name;
            }
        }

        nameMatcher = NAME_FINAL_PATTERN.matcher(script);
        if (nameMatcher.find()) {
            String name = nameMatcher.group(1);
            if (fullPackage == null) {
                return name;
            } else {
                return fullPackage + "." + name;
            }
        }

        return "scripts." + file.getName().substring(0, file.getName().length() - ".java".length());
    }

    public static List<File> traverse(File root) {
        return traverse(root, null);
    }

    public static List<File> traverse(File root, Predicate<File> filter) {
        List<File> list = new ArrayList<>();
        traverse(root, list, filter);
        return list;
    }

    private static void traverse(File root, List<File> files, Predicate<File> filter) {
        File[] list = root.listFiles();
        if (list == null || list.length == 0) {
            return;
        }

        for (File f : list) {
            if (filter == null || filter.test(f)) {
                files.add(f);
            }
            if (f.isDirectory()) {
                traverse(f, files, filter);
            }
        }
    }

    public static String readData(File file) {
        try (Scanner scanner = new Scanner(file)) {
            StringBuilder contents = new StringBuilder();
            while (scanner.hasNextLine()) {
                contents.append(scanner.nextLine()).append("\n");
            }
            return contents.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
