# Renames

Этп программа удаляет из названия всех файлов в заданной дирректории префикс.
Диррекирия и префикс заданы в файле настроек config.properties и называются соотвественно
- directory
- prefix
  Префикс указан в кавычках для того чтобы можно было удалять из названия пробельные символы.
  Давай улучшим данную программу добавив в нее возможность переименовывать файлы не только в текущей дирректории, но и во всех вложеннных дирректориях по указанному пути.

FileInDirsRenamer - версия прораммы которая выолняем перименование в поддиректиях


javac -d bin G:\\Programmers\\rmnames\\FileRenamer.java
javac -d bin G:/Programmers/rmnames/FileRenamer.java


javac -d G:\\Prpgrammers\\rmnames\\bin G:\\Prpgrammers\\rmnames\\src\\FileRenamer.java
java -cp bin FileRenamer
java -cp G:\\Prpgrammers\\rmnames\\bin FileRenamer

Как использовать:

- По умолчанию: java FileRenamer
- С указанием конфига: java FileRenamer --config /path/to/config.properties


Работает:

java -cp out FileRenamer --config G:/Programmers/rmnames/config.properties

directory=G:\\Programmers\\LLM\\for_Images\\RFOLD
prefix="[SRT]"

Запуск:
Из рабочейй дирректории
2. javac -d out src/FileRenamer.java
3. java -cp out FileRenamer --config config.properties

Запуск из любой дирректории, все файлы находтя в ней
javac -d out src/FileInDirsRenamer.java
java FileInDirsRenamer --config config.properties
