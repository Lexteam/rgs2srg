package uk.jamierocks.mappingsgen;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

public class MappingsGen {

    private static final File classRules = new File("input/client.rules");
    private static final File classOutput = new File("output/classes.srg");

    private static BiMap<String, String> classMappings;

    public static void main(String[] args) throws IOException {
        FileInputStream fis = new FileInputStream(classRules);
        //if (!classOutput.exists()) classOutput.createNewFile();
        FileOutputStream fos = new FileOutputStream(classOutput);
        Scanner scanner = new Scanner(fis);

        ImmutableBiMap.Builder b = ImmutableBiMap.<String, String>builder();

        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            line = line.replace("rule ", "");

            String[] mapping = line.split(" ");

            String classLine = String.format("CL: %s %s\n", mapping[0], mapping[1]);
            byte[] data = classLine.getBytes();

            if (!classLine.contains("@")) {
                b.put(mapping[0].replace(" ", ""), mapping[1].replace(" ", ""));
                fos.write(data, 0, data.length);
            }
        }

        classMappings = b.build();

        fis.close();
        fos.close();
    }
}
