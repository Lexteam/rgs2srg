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

public class MappingsConverter {

    // These are the input files
    private static final File classRgs = new File("input/server.rgs");
    // This is the file to output to
    private static final File classOutput = new File("output/server.srg");

    // This map stores the deobf -> obf class names
    private static Map<String, String> deobfMappings = Maps.newHashMap();
    private static Map<String, String> obfMappings = Maps.newHashMap();

    // These Lists are used to seperate the methods and fields in the output file
    private static List<String> classLines = Lists.newArrayList();
    private static List<String> fieldLines = Lists.newArrayList();
    private static List<String> methodLines = Lists.newArrayList();

    public static void main(String[] args) throws IOException {
        FileInputStream rgs = new FileInputStream(classRgs);
        FileOutputStream srgFile = new FileOutputStream(classOutput);
        Scanner rgsScanner = new Scanner(rgs);

        while(rgsScanner.hasNext()) {
            String line = rgsScanner.nextLine();
            if (line.startsWith(".class_map ")) {
                line = line.replace(".class_map ", "");

                String[] mappings = line.split(" ");

                String classLine = String.format("CL: %s %s\n", mappings[0].replace(".", "/"), mappings[1].replace(".", "/"));
                if (!classLine.contains("@")) {
                    deobfMappings.put(mappings[1].replace(".", "/"), mappings[0].replace(".", "/"));
                    obfMappings.put(mappings[0].replace(".", "/"), mappings[1].replace(".", "/"));
                    classLines.add(classLine);
                }
            } else if (line.startsWith(".field_map ")) {
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

                if (!lastOriginal.equalsIgnoreCase(mappings[2]) && !methodLine.contains("$")) {
                    methodLines.add(methodLine);
                }
            }
        }

        for (String classLine : classLines) {
            srgFile.write(classLine.getBytes());
        }

        for  (String fieldLine : fieldLines) {
            srgFile.write(fieldLine.getBytes());
        }

        for (String methodLine : methodLines) {
            srgFile.write(methodLine.getBytes());
        }

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
