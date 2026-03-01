import java.io.InputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class FileInDirsRenamer {

    public static void main(String[] args) throws IOException {
        String configPath = resolveConfigPath(args);
        Properties properties = loadProperties(configPath);
        printConfigInfo(configPath, properties);

        String directoryPath = requireTrimmed(properties, "directory");
        String prefix = parseConfigValue(properties, "prefix");

        System.out.println("Директория: " + directoryPath);
        System.out.println("Префикс для удаления: " + prefix);

        Path dir = Paths.get(directoryPath);
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Указанная директория не найдена: " + directoryPath);
        }

        if (prefix.isEmpty()) {
            System.out.println("Префикс пустой. Переименование не требуется.");
            return;
        }

        int scanned = 0;
        int renamed = 0;
        int skipped = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path file : stream) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                scanned++;
                String filename = file.getFileName().toString();
                if (!filename.startsWith(prefix)) {
                    continue;
                }

                String newFilename = filename.substring(prefix.length());
                if (newFilename.isEmpty()) {
                    System.err.println("Пропуск: имя файла после удаления префикса пустое: " + filename);
                    skipped++;
                    continue;
                }

                Path target = file.resolveSibling(newFilename);
                try {
                    Files.move(file, target);
                    System.out.println("Переименован файл: " + filename + " → " + newFilename);
                    renamed++;
                } catch (FileAlreadyExistsException e) {
                    System.err.println("Пропуск: целевой файл уже существует: " + newFilename);
                    skipped++;
                } catch (IOException e) {
                    System.err.println("Ошибка при переименовании файла: " + filename + ". Причина: " + e.getMessage());
                    skipped++;
                }
            }
        }

        if (scanned == 0) {
            System.out.println("Нет файлов для обработки в директории.");
        } else {
            System.out.println("Готово. Переименовано: " + renamed + ", пропущено: " + skipped + ".");
        }
    }

    private static String resolveConfigPath(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("--config".equals(args[i])) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("Не указан путь после --config.");
                }
                String value = args[i + 1].trim();
                if (value.isEmpty()) {
                    throw new IllegalArgumentException("Путь после --config не должен быть пустым.");
                }
                return value;
            }
        }
        return "config.properties";
    }

    private static Properties loadProperties(String filePath) throws IOException {
        Path configPath = Paths.get(filePath);
        if (!Files.exists(configPath)) {
            throw new IOException("Файл '" + filePath + "' не найден!");
        }
        Properties props = new Properties();
        try (InputStream inputStream = Files.newInputStream(configPath)) {
            props.load(inputStream);
        }
        return props;
    }

    private static void printConfigInfo(String filePath, Properties properties) throws IOException {
        Path absolute = Paths.get(filePath).toAbsolutePath().normalize();
        System.out.println("Конфиг: " + absolute);
        System.out.println("Ключи конфига:");
        for (String key : properties.stringPropertyNames()) {
            System.out.println("- " + key);
        }
    }

    private static String requireTrimmed(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Отсутствует параметр в конфиге: " + key);
        }
        return value.trim();
    }

    private static String parseConfigValue(Properties properties, String key) {
        String value = requireTrimmed(properties, key);
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

}
