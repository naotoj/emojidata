import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        Files.readAllLines(Path.of("../../dev/jdk/git/master/open/src/java.base/share/data/unicodedata/emoji/emoji-data.txt"))
                .stream()
                .forEach(System.out::println);
    }
}