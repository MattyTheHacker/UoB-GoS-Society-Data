package utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileHandler {
    private static final int[] INVALID_CHARACTERS = {34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47};

    static { Arrays.sort(INVALID_CHARACTERS);}

    public static boolean fileExists(String path) {
        return new File(path).exists();
    }

    public static Set<String> listFiles(String directory) {
        return Stream.of(Objects.requireNonNull(new File(directory).listFiles()))
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet());
    }

    public static boolean saveStringToFile(String content, String path){
        try (FileWriter fileWriter = new FileWriter(path)) {
            fileWriter.write(content);
            System.out.println("[INFO] Saved file to " + path + ".");
            return true;
        } catch (java.io.IOException e) {
            System.out.println("[ERROR] Failed to save file to " + path + ".");
            System.out.println("[DEBUG]" + e.getMessage());
            return false;
        }
    }

    public static String loadFromFile(String path) {
        if (!fileExists(path)) {
            System.out.println("[ERROR] File " + path + " does not exist.");
            return null;
        }
        try (FileReader fileReader = new FileReader(path)) {
            StringBuilder stringBuilder = new StringBuilder();
            int i;
            while ((i = fileReader.read()) != -1) {
                stringBuilder.append((char) i);
            }
            return stringBuilder.toString();
        } catch (java.io.IOException e) {
            System.out.println("[ERROR] Failed to load file from " + path + ".");
            return null;
        }
    }

    public static String SanitiseFileName(String filename) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filename.length(); i++) {
            int c = (int) filename.charAt(i);
            if (Arrays.binarySearch(INVALID_CHARACTERS, c) < 0) {
                sb.append((char) c);
            }
        }
        return sb.toString();
    }
}
