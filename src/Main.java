import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main {
    static final boolean[] elseif = new boolean[1];

    public static void main(String[] args) throws Exception {
        var THRESHOLD_CP = 0x2000;
        var emojis =
            Stream.concat(
                // Fast path for non-emoji below THRESHOLD_CP, some of those are overridden below
                Stream.of("00..%s ; Non_Emoji".formatted(Integer.toHexString(THRESHOLD_CP))),

                // Others from emoji-data.txt
                Files.readAllLines(Path.of("../../dev/jdk/git/master/open/src/java.base/share/data/unicodedata/emoji/emoji-data.txt"))
                    .stream()
                    .map(line -> line.split("#", 2)[0])
                    .filter(Predicate.not(String::isBlank)))
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

//
//        try (var oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("emojis.ser")))) {
//            oos.writeObject(emojis);
//        }
//
//        Map<Integer, Integer> deserEmojis ;
//        try (var ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream("emojis.ser")))) {
//            deserEmojis = (Map<Integer, Integer>)ois.readObject();
//        }
//
//        deserEmojis.keySet()
//                .stream()
//                .sorted()
//                .forEach(cp -> {
//            System.out.print("""
//                    0x%x (%s): 0x%x
//                    """.formatted(cp, Character.toString(cp), deserEmojis.get(cp)));
//        });

            var reversed = emojis.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getValue(),
                        e -> new TreeSet<>(Set.of(e.getKey())),
                        (v1, v2) -> {v1.addAll(v2); return v1;},
                        TreeMap::new));

        System.out.print("""
            int getType(int cp) {
            """);

        // BMP
        printConditions(reversed, true, THRESHOLD_CP);

        System.out.print("""
            
                // cp > 0x%x
            """.formatted(THRESHOLD_CP));

        // non-BMP
        printConditions(reversed, false, THRESHOLD_CP);
        System.out.print("""
                else return 0;
            }
            """);
    }

    static void printConditions(Map<Integer, ? extends SortedSet<Integer>> m, boolean isLow, int threshold) {
        final Integer[] start = new Integer[1];
        final Integer[] end = new Integer[1];
        m.forEach((k,v) -> {
            // accumulate conditions
            var conds = new ArrayList<String>();
            v.forEach(cp -> {
                if (start[0] == null) {
                    if (isLow ^ cp < threshold) {
                        return;
                    }
                    start[0] = cp;
                } else {
                    if (end[0] == null && start[0].intValue() + 1 == cp.intValue() ||
                            end[0] != null && end[0].intValue() + 1 == cp.intValue()) {
                        end[0] = cp;
                    } else {
                        if (end[0] != null) {
                            conds.add("cp >= 0x%x && cp <= 0x%x".formatted(start[0], end[0]));

                        } else {
                            conds.add("cp == 0x%x".formatted(start[0]));
                        }
                        start[0] = null;
                        end[0] = null;
                    }
                }
            });
            if (!conds.isEmpty()) {
                var condsStr = conds.stream().collect(Collectors.joining(" ||\n        "));
                if (elseif[0]) {
                    System.out.print("""
                            else if (%s) 
                                return 0x%x;
                        """.formatted(condsStr, k));
                } else {
                    System.out.print("""
                        if (%s) 
                            return 0x%x;
                    """.formatted(condsStr, k));
                    elseif[0] = true;
                }
            }
        });
    }

    private static final int NON_EMOJI = 0x00000000;
    private static final int EMOJI = 0x00000001;
    private static final int EMOJI_PRESENTATION = 0x00000002;
    private static final int EMOJI_MODIFIER = 0x00000004;
    private static final int EMOJI_MODIFIER_BASE = 0x00000008;
    private static final int EMOJI_COMPONENT = 0x00000010;
    private static final int EXTENDED_PICTOGRAPHIC = 0x00000020;
    static int convertType(String type) {
        return switch (type) {
            case "Non_Emoji" -> NON_EMOJI;
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