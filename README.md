# Renames

Программа переименовывает файлы в заданной директории. Расширенные режимы работают через `FileInDirsRenamer`.

## Параметры конфига

- `directory` - путь к директории для обработки.
- `mode` - режим работы для `FileInDirsRenamer`.
- `prefix` - префикс для удаления (нужен только для режима удаления префикса).

`prefix` можно указывать в кавычках, чтобы сохранять пробелы и спецсимволы.

## Режимы (`mode`) для `FileInDirsRenamer`

- `strip-prefix-recursive` - удалить префикс у файлов в директории и всех поддиректориях.
- `date-ddmmyy-to-yymmdd-recursive` - рекурсивно переименовать файлы, имя которых начинается с даты формата `dd.MM.yy`, в формат `yy.MM.dd`.

Пример для нового режима даты:

- Было: `19.05.17. Игорь Пыхалов про евросоюз, часть первая.txt`
- Станет: `17.05.19. Игорь Пыхалов про евросоюз, часть первая.txt`

## Пример `config.properties`

### 1) Удаление префикса рекурсивно

```properties
directory=G:\\Prpgrammers\\LLM\\for_Images\\RFOLD\\
mode=strip-prefix-recursive
prefix="[SW.BAND]"
```

### 2) Перестановка даты рекурсивно

```properties
directory=G:\\Prpgrammers\\LLM\\for_Images\\RFOLD\\
mode=date-ddmmyy-to-yymmdd-recursive
prefix=""
```

## Сборка и запуск

```bash
javac -d out src/main/java/com/FileInDirsRenamer.java
java -cp out src.main.java.com.FileInDirsRenamer --config config.properties
```

Для Windows (с вашим путем проекта) можно собрать и создать `bat`-файл, который запускается из любой директории:

```bat
cd /d G:\Prpgrammers\myPrj\Renames
javac -d out G:\Prpgrammers\myPrj\Renames\src\main\java\com\FileInDirsRenamer.java
```

Содержимое `run-renamer.bat`:

```bat
@echo off
java -cp "G:\Prpgrammers\myPrj\Renames\out" src.main.java.com.FileInDirsRenamer --config "%~1"
```

Пример запуска из любой директории (Windows):

```bat
G:\Prpgrammers\myPrj\Renames\run-renamer.bat G:\Prpgrammers\myPrj\Renames\config.properties
```

`FileRenamer` не изменен: это отдельный старый режим удаления префикса только в текущей директории.
