package uk.jamierocks.mappingsgen;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MappingsGen {

    // These are the input files
    private static final File classRules = new File("input/client.rules");
    private static final File classRgs = new File("input/client.rgs");
    // This is the file to output to
    private static final File classOutput = new File("output/classes.srg");

    // This map stores the deobf -> obf class names
    private static Map<String, String> deobfMappings = Maps.newHashMap();
    private static Map<String, String> obfMappings = Maps.newHashMap();

    // These Lists are used to seperate the methods and fields in the output file
    private static List<String> fieldLines = Lists.newArrayList();
    private static List<String> methodLines = Lists.newArrayList();

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
                deobfMappings.put(mapping[1].replace(".", "/"), mapping[0].replace(".", "/"));
                obfMappings.put(mapping[0].replace(".", "/"), mapping[1].replace(".", "/"));
                srgFile.write(data, 0, data.length);
            }
        }

        while(rgsScanner.hasNext()) {
            String line = rgsScanner.nextLine();
            if (line.startsWith(".field_map ")) {
                line = line.replace(".field_map ", "");

                String[] mappings = line.split(" ");

                String original = getOriginalMapping(mappings[0]);
                String modified = getModifiedMapping(mappings[0], mappings[1]);

                String fieldLine = String.format("FD: %s %s\n", original, modified);

                String[] originalSplit = original.split("/");
                String lastOriginal = originalSplit[originalSplit.length-1];

                if (!lastOriginal.equalsIgnoreCase(mappings[1]) && !fieldLine.contains("$")) {
                    fieldLines.add(fieldLine);
                }
            } else if(line.startsWith(".method_map ")) {
                line = line.replace(".method_map ", "");

                String[] mappings = line.split(" ");

                String original = getOriginalMapping(mappings[0]);
                String originalType = getOriginalType(mappings[1]);
                String modified = getModifiedMapping(mappings[0], mappings[2]);
                String modifiedType = mappings[1];

                String methodLine = String.format("MD: %s %s %s %s\n", original, originalType, modified, modifiedType);

                String[] originalSplit = original.split("/");
                String lastOriginal = originalSplit[originalSplit.length-1];

                if (!lastOriginal.equalsIgnoreCase(mappings[2])) {
                    methodLines.add(methodLine);
                }
            }
        }

        for  (String fieldLine : fieldLines) {
            srgFile.write(fieldLine.getBytes());
        }

        for (String methodLine : methodLines) {
            srgFile.write(methodLine.getBytes());
        }

        rules.close();
        srgFile.close();
    }

    public static String getModifiedMapping(String originalMapping, String newMapping) {
        String[] split = originalMapping.split("/");
        int lastIndex = originalMapping.lastIndexOf(split[split.length-1]);

        return originalMapping.substring(0, lastIndex) + newMapping;
    }

    public static String getOriginalMapping(String modifiedMapping) {
        String[] split = modifiedMapping.split("/");
        int lastIndex = modifiedMapping.lastIndexOf(split[split.length-1]);

        String className = modifiedMapping.substring(0, lastIndex - 1);
        if (deobfMappings.containsKey(className)) {
            className = deobfMappings.get(className);
        }

        String mapping = modifiedMapping.substring(lastIndex);

        return className + "/" + mapping;
    }

    public static String getOriginalType(String modifiedType) {
        String innerContent = modifiedType.substring(modifiedType.indexOf("(") + 1, modifiedType.indexOf(")"));
        String outerContent = modifiedType.substring(modifiedType.indexOf(")") + 1);

        String originalType = modifiedType;

        for (String type : innerContent.split(";")) {
            if (type.startsWith("L")) {
                String newType = type.substring(1);
                if (deobfMappings.containsKey(newType)) {
                    originalType = originalType.replace(newType, deobfMappings.get(newType));
                }
            }
        }

        if (outerContent.startsWith("L")) {
            String outerType = outerContent.substring(1, outerContent.length() - 1);
            if (deobfMappings.containsKey(outerType)) {
                originalType = originalType.replace(outerType, deobfMappings.get(outerType));
            }
        }

        return originalType;
    }
}
