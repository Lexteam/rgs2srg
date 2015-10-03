package uk.jamierocks.mappingsgen;

import com.google.common.collect.Maps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

public class MappingsGen {

    // These are the input files
    private static final File classRules = new File("input/client.rules");
    private static final File classRgs = new File("input/client.rgs");
    // This is the file to output to
    private static final File classOutput = new File("output/classes.srg");

    // This map stores the deobf -> obf class names
    private static Map<String, String> classMappings = Maps.newHashMap();

    public static void main(String[] args) throws IOException {
        FileInputStream rules = new FileInputStream(classRules);
        FileInputStream rgs = new FileInputStream(classRgs);
        FileOutputStream srgFile = new FileOutputStream(classOutput);
        Scanner rulesScanner = new Scanner(rules);
        Scanner rgsScanner = new Scanner(rgs);

        while (rulesScanner.hasNext()) {
            String line = rulesScanner.nextLine();
            line = line.replace("rule ", "");

            String[] mapping = line.split(" ");

            String classLine = String.format("CL: %s %s\n", mapping[0].replace(".", "/"), mapping[1].replace(".", "/"));
            byte[] data = classLine.getBytes();

            if (!classLine.contains("@")) {
                classMappings.put(mapping[1].replace(".", "/"), mapping[0].replace(".", "/"));
                srgFile.write(data, 0, data.length);
            }
        }

        while(rgsScanner.hasNext()) {
            String line = rgsScanner.nextLine();
            if (line.startsWith(".field_map ")) {
                line = line.replace(".field_map ", "");

                String[] mapping = line.split(" ");

                String[] thingy = mapping[0].split("/");
                String thing = thingy[thingy.length-1];

                String className = mapping[0].replace("/" + thing, "");

                String gg = classMappings.get(className);
                if (gg == null || gg.equals("")) {
                    gg = className;
                }

                String obfuscated = gg + "/" + thing;
                System.out.println(className);
                System.out.println(gg);
                System.out.println(obfuscated);
                System.out.println(" ");
                String deobfuscated = className + "/" + mapping[1];

                String fieldLine = String.format("FD: %s %s\n", obfuscated, deobfuscated);
                byte[] data = fieldLine.getBytes();

                if (!fieldLine.contains("$") && !thing.equalsIgnoreCase(mapping[1])) {
                    srgFile.write(data, 0, data.length);
                }
            } else if(line.startsWith(".method_map ")) {
                line = line.replace(".method_map ", "");

                String[] mappings = line.split(" ");

                String original = getOriginalMapping(mappings[0]);
                String originalType = mappings[1]; // todo:
                String modified = getModifiedMapping(mappings[0], mappings[2]);
                String modifiedType = mappings[1];

                String methodLine = String.format("MD: %s %s %s %s\n", original, originalType, modified, modifiedType);
                srgFile.write(methodLine.getBytes()); //todo: check if they are the same
            }
        }

        rules.close();
        srgFile.close();
    }

    public static String getModifiedMapping(String originalMapping, String newMapping) {
        String[] split = originalMapping.split("/");
        String lastSplit = split[split.length-1];
        String className = originalMapping.replace("/" + lastSplit, "");

        return className + "/" + newMapping;
    }

    public static String getOriginalMapping(String modifiedMapping) {
        String[] split = modifiedMapping.split("/");
        String lastSplit = split[split.length-1];
        String className = modifiedMapping.replace("/" + lastSplit, "");

        String originalClassName = classMappings.get(className);
        if (originalClassName == null || originalClassName.equals("")) {
            originalClassName = className;
        }

        return originalClassName + "/" + lastSplit;
    }
}
