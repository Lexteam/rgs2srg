package uk.jamierocks.mappingsgen;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class MappingsGen {

    private static final File classRules = new File("input/client.rules");
    private static final File classRgs = new File("input/client.rgs");
    private static final File classOutput = new File("output/classes.srg");

    private static Map<String, String> classMappings = new HashMap<>();
    private static Map<String, String> fieldMappings = new HashMap<>();

    public static void main(String[] args) throws IOException {
        FileInputStream rules = new FileInputStream(classRules);
        FileInputStream rgs = new FileInputStream(classRgs);
        //if (!classOutput.exists()) classOutput.createNewFile();
        FileOutputStream fos = new FileOutputStream(classOutput);
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
                fos.write(data, 0, data.length);
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
                System.out.println(" ");
                String deobfuscated = className + "/" + mapping[1];

                String fieldLine = String.format("FD: %s %s\n", obfuscated, deobfuscated);
                byte[] data = fieldLine.getBytes();

                if (!fieldLine.contains("$") && !thing.equalsIgnoreCase(mapping[1])) {
                    fieldMappings.put(deobfuscated, obfuscated);
                    fos.write(data, 0, data.length);
                }
            }
        }

        rules.close();
        fos.close();
    }
}
