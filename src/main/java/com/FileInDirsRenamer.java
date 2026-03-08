package src.main.java.com;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class FileInDirsRenamer {

    private static final Pattern START_DATE_PATTERN = Pattern.compile("^(\\d{2})\\.(\\d{2})\\.(\\d{2})(\\..*)$");

    private enum Mode {
        STRIP_PREFIX_RECURSIVE,
        DATE_DDMMYY_TO_YYMMDD_RECURSIVE;

        static Mode fromConfig(String rawValue) {
            String value = rawValue.trim().toLowerCase();
            switch (value) {
                case "strip-prefix-recursive":
                    return STRIP_PREFIX_RECURSIVE;
                case "date-ddmmyy-to-yymmdd-recursive":
                    return DATE_DDMMYY_TO_YYMMDD_RECURSIVE;
                default:
                    throw new IllegalArgumentException(
                            "Неизвестный режим mode='" + rawValue + "'. Допустимые значения: "
                                    + "strip-prefix-recursive, date-ddmmyy-to-yymmdd-recursive");
            }
        }
    }

    private static final class RenameStats {
        private int scanned;
        private int renamed;
        private int skipped;
    }

    public static void main(String[] args) throws IOException {
        String configPath = resolveConfigPath(args);
        Properties properties = loadProperties(configPath);
        printConfigInfo(configPath, properties);

        String directoryPath = requireTrimmed(properties, "directory");
        String modeRaw = properties.getProperty("mode", "strip-prefix-recursive").trim();
        Mode mode = Mode.fromConfig(modeRaw);

        System.out.println("Директория: " + directoryPath);
        System.out.println("Режим: " + modeRaw);

        Path dir = Paths.get(directoryPath);
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Указанная директория не найдена: " + directoryPath);
        }

        RenameStats stats;
        switch (mode) {
            case STRIP_PREFIX_RECURSIVE:
                stats = renameStripPrefixRecursively(dir, parseConfigValue(properties, "prefix"));
                break;
            case DATE_DDMMYY_TO_YYMMDD_RECURSIVE:
                stats = renameDateInFilenameRecursively(dir);
                break;
            default:
                throw new IllegalStateException("Режим не обработан: " + mode);
        }

        if (stats.scanned == 0) {
            System.out.println("Нет файлов для обработки в директории и вложенных каталогах.");
        } else {
            System.out.println("Готово. Просканировано: " + stats.scanned + ", переименовано: " + stats.renamed
                    + ", пропущено: " + stats.skipped + ".");
        }
    }

    private static RenameStats renameStripPrefixRecursively(Path dir, String prefix) throws IOException {
        System.out.println("Префикс для удаления: " + prefix);
        if (prefix.isEmpty()) {
            System.out.println("Префикс пустой. Переименование не требуется.");
            return new RenameStats();
        }

        RenameStats stats = new RenameStats();
        try (Stream<Path> stream = Files.walk(dir)) {
            for (Path file : (Iterable<Path>) stream::iterator) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                stats.scanned++;
                renameByPrefix(file, prefix, stats);
            }
        }
        return stats;
    }

    private static RenameStats renameDateInFilenameRecursively(Path dir) throws IOException {
        RenameStats stats = new RenameStats();
        try (Stream<Path> stream = Files.walk(dir)) {
            for (Path file : (Iterable<Path>) stream::iterator) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                stats.scanned++;

                String filename = file.getFileName().toString();
                Matcher matcher = START_DATE_PATTERN.matcher(filename);
                if (!matcher.matches()) {
                    continue;
                }

                String day = matcher.group(1);
                String month = matcher.group(2);
                String year = matcher.group(3);
                String suffix = matcher.group(4);

                String newFilename = year + "." + month + "." + day + suffix;
                Path target = file.resolveSibling(newFilename);
                tryMove(file, target, stats);
            }
        }
        return stats;
    }

    private static void renameByPrefix(Path file, String prefix, RenameStats stats) {
        String filename = file.getFileName().toString();
        if (!filename.startsWith(prefix)) {
            return;
        }

        String newFilename = filename.substring(prefix.length());
        if (newFilename.isEmpty()) {
            System.err.println("Пропуск: имя файла после удаления префикса пустое: " + file);
            stats.skipped++;
            return;
        }

        Path target = file.resolveSibling(newFilename);
        tryMove(file, target, stats);
    }

    private static void tryMove(Path source, Path target, RenameStats stats) {
        try {
            Files.move(source, target);
            System.out.println("Переименован файл: " + source + " -> " + target);
            stats.renamed++;
        } catch (FileAlreadyExistsException e) {
            System.err.println("Пропуск: целевой файл уже существует: " + target);
            stats.skipped++;
        } catch (IOException e) {
            System.err.println("Ошибка при переименовании файла: " + source + ". Причина: " + e.getMessage());
            stats.skipped++;
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

    private static void printConfigInfo(String filePath, Properties properties) {
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
