/*
 * This file is part of rgs2srg, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015, Jamie Mansfield <https://github.com/jamierocks>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package uk.jamierocks.rgs2srg;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Convert *.rgs mappings to *.srg mappings.
 */
public class MappingsConverter {

    private final File input;
    private final File output;

    // This map stores the deobf -> obf class names
    private Map<String, String> deobfMappings = Maps.newHashMap();
    private Map<String, String> obfMappings = Maps.newHashMap();

    // These Lists are used to seperate the methods and fields in the output file
    private List<String> classLines = Lists.newArrayList();
    private List<String> fieldLines = Lists.newArrayList();
    private List<String> methodLines = Lists.newArrayList();

    public MappingsConverter(File input, File output) {
        this.input = input;
        this.output = output;

        if (!input.exists()) {
            System.out.println("Input doesn't exist! Exiting...");
            System.exit(0);
        }
        if (!output.exists()) {
            System.out.println("Output doesn't exist! Creating...");
            try {
                if (!output.createNewFile()) {
                    System.out.println("Could not create output! Exiting...");
                    System.exit(0);
                } else {
                    System.out.println("Successfully created output.");
                }
            } catch (IOException ex) {
                System.out.println("Oh crap! Something bad has happened. Exiting...");
                ex.printStackTrace();
                System.exit(0);
            }
        }
    }

    public void convert() throws IOException {
        FileInputStream rgs = new FileInputStream(this.input);
        FileOutputStream srgFile = new FileOutputStream(this.output);
        Scanner rgsScanner = new Scanner(rgs);

        List<String> delayedReadings = Lists.newArrayList();

        while(rgsScanner.hasNext()) {
            String line = rgsScanner.nextLine();
            if (line.startsWith(".class_map ")) {
                line = line.replace(".class_map ", "");

                String[] mappings = line.split(" ");

                String classLine = String.format("CL: %s %s\n", mappings[0].replace(".", "/"), mappings[1].replace(".", "/"));
                if (!classLine.contains("@")) {
                    this.deobfMappings.put(mappings[1].replace(".", "/"), mappings[0].replace(".", "/"));
                    this.obfMappings.put(mappings[0].replace(".", "/"), mappings[1].replace(".", "/"));
                    this.classLines.add(classLine);
                }
            } else {
                delayedReadings.add(line);
            }
        }

        for (String line : delayedReadings) {
            if (line.startsWith(".field_map ")) {
                line = line.replace(".field_map ", "");

                String[] mappings = line.split(" ");

                String original = mappings[0];
                String modified = this.getModifiedMapping(mappings[0], mappings[1]);

                String fieldLine = String.format("FD: %s %s\n", original, modified);

                String[] originalSplit = original.split("/");
                String lastOriginal = originalSplit[originalSplit.length-1];

                if (!lastOriginal.equalsIgnoreCase(mappings[1]) && !fieldLine.contains("$")) {
                    this.fieldLines.add(fieldLine);
                }
            } else if(line.startsWith(".method_map ")) {
                line = line.replace(".method_map ", "");

                String[] mappings = line.split(" ");

                String original = mappings[0];
                String originalType = mappings[1];
                String modified = this.getModifiedMapping(mappings[0], mappings[2]);
                String modifiedType = this.getModifiedType(mappings[1]);

                String methodLine = String.format("MD: %s %s %s %s\n", original, originalType, modified, modifiedType);

                String[] originalSplit = original.split("/");
                String lastOriginal = originalSplit[originalSplit.length-1];

                if (!lastOriginal.equalsIgnoreCase(mappings[2]) && !methodLine.contains("$")) {
                    this.methodLines.add(methodLine);
                }
            }
        }

        for (String classLine : this.classLines) {
            srgFile.write(classLine.getBytes());
        }

        for  (String fieldLine : this.fieldLines) {
            srgFile.write(fieldLine.getBytes());
        }

        for (String methodLine : this.methodLines) {
            srgFile.write(methodLine.getBytes());
        }

        rgs.close();
        srgFile.close();
    }

    private String getModifiedMapping(String originalMapping, String newMapping) {
        String[] split = originalMapping.split("/");

        String className = originalMapping.substring(0, split[split.length-1].length());
        if (this.obfMappings.containsKey(className)) {
            className = this.obfMappings.get(className);
        }

        return className + "/" + newMapping;
    }

    private String getModifiedType(String originalType) {
        String innerContent = originalType.substring(originalType.indexOf("(") + 1, originalType.indexOf(")"));
        String outerContent = originalType.substring(originalType.indexOf(")") + 1);

        String modifiedType = originalType;

        for (String type : innerContent.split(";")) {
            if (type.startsWith("L")) {
                String newType = type.substring(1);
                if (this.obfMappings.containsKey(newType)) {
                    modifiedType = modifiedType.replace(newType, this.obfMappings.get(newType));
                }
            }
        }

        if (outerContent.startsWith("L")) {
            String outerType = outerContent.substring(1, outerContent.length() - 1);
            if (this.obfMappings.containsKey(outerType)) {
                modifiedType = modifiedType.replace(outerType, this.obfMappings.get(outerType));
            }
        }

        return modifiedType;
    }
}
