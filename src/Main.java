import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) throws Exception {
        var emojis = Files.readAllLines(Path.of("../../dev/jdk/git/master/open/src/java.base/share/data/unicodedata/emoji/emoji-data.txt"))
                .stream()
                .map(line -> line.split("#", 2)[0])
                .filter(Predicate.not(String::isBlank))
                .map(line -> line.split("[ \t]*;[ \t]*", 2))
                .flatMap(map -> {
                    var range = map[0].split("\\.\\.", 2);
                    var start = Integer.valueOf(range[0], 16);
                    return range.length == 1 ?
                        Stream.of(new AbstractMap.SimpleEntry<>(start, convertType(map[1].trim()))) :
                        IntStream.rangeClosed(start,
                                Integer.valueOf(range[1], 16))
                                .mapToObj(cp -> new AbstractMap.SimpleEntry<>(cp, convertType(map[1].trim())));

                })
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue, (v1, v2) -> v1 | v2));
//        emojis.keySet().forEach(System.out::println);
//        var type = (int)emojis.get(0x4e00);
//        System.out.println(type & EMOJI_PRESENTATION);
        System.out.print("""
                var emojiMap = Map.ofEntries(
                """);
        emojis.forEach((k, v) -> {
            System.out.print("""
                        Map.entry(0x%x, 0x%x),
                    """.formatted(k, v));
        });
        System.out.print("""
                        Map.entry(0, 0) // dummy
                    );
                    """);
    }

    private static final int EMOJI = 0x00000001;
    private static final int EMOJI_PRESENTATION = 0x00000002;
    private static final int EMOJI_MODIFIER = 0x00000004;
    private static final int EMOJI_MODIFIER_BASE = 0x00000008;
    private static final int EMOJI_COMPONENT = 0x00000010;
    private static final int EXTENDED_PICTOGRAPHIC = 0x00000020;
    static int convertType(String type) {
        return switch (type) {
            case "Emoji" -> EMOJI;
            case "Emoji_Presentation" -> EMOJI_PRESENTATION;
            case "Emoji_Modifier" -> EMOJI_MODIFIER;
            case "Emoji_Modifier_Base" -> EMOJI_MODIFIER_BASE;
            case "Emoji_Component" -> EMOJI_COMPONENT;
            case "Extended_Pictographic" -> EXTENDED_PICTOGRAPHIC;
            default -> throw new InternalError();
        };
    }
}