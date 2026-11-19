import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Pemakaian:");
            System.out.println("  c <file_asli> <file.huf>      kompres");
            System.out.println("  d <file.huf> <file_hasil>     dekompres");
            System.out.println("  t <file_asli>                 uji round trip");
            return;
        }
        String mode = args[0];
        File a = new File(args[1]);

        if (mode.equals("c")) {
            System.out.println(FileCompressor.compress(a, new File(args[2])));
        } else if (mode.equals("d")) {
            System.out.println(FileCompressor.decompress(a, new File(args[2])));
        } else if (mode.equals("t")) {
            File huf = new File(a.getPath() + ".huf");
            File balik = new File(a.getPath() + ".balik");
            System.out.println(FileCompressor.compress(a, huf));
            System.out.println(FileCompressor.decompress(huf, balik));
            boolean sama = Arrays.equals(Files.readAllBytes(a.toPath()),
                                         Files.readAllBytes(balik.toPath()));
            System.out.println(sama ? "ROUND TRIP: IDENTIK" : "ROUND TRIP: BEDA (ada bug!)");
        }
    }
}