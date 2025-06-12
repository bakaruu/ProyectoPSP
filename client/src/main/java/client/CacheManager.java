package client;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Gestiona la caché de LoginData en un fichero de texto:
 * cada línea tiene formato nick|ip|puerto
 */
public class CacheManager {
    private static final Path CACHE_FILE =
            Paths.get(System.getProperty("user.home"), ".chatapp_cache");

    /** Lee el cache y devuelve una lista de String[3]={nick,ip,port} */
    public static List<String[]> load() {
        List<String[]> list = new ArrayList<>();
        if (Files.exists(CACHE_FILE)) {
            try (BufferedReader r = Files.newBufferedReader(CACHE_FILE)) {
                String line;
                while ((line = r.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length == 3) {
                        list.add(parts);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    /** Añade (si no existe) y guarda la caché completa */
    public static void addAndSave(String nick, String ip, int port) {
        List<String[]> list = load();
        String key = nick + "|" + ip + "|" + port;
        boolean exists = list.stream()
                .anyMatch(arr -> (arr[0] + "|" + arr[1] + "|" + arr[2]).equals(key));
        if (!exists) {
            list.add(new String[]{nick, ip, String.valueOf(port)});
            save(list);
        }
    }

    private static void save(List<String[]> list) {
        try (BufferedWriter w = Files.newBufferedWriter(CACHE_FILE)) {
            for (String[] arr : list) {
                w.write(arr[0] + "|" + arr[1] + "|" + arr[2]);
                w.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
