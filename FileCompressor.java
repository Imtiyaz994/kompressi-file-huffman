import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;

class FileCompressor {
    private static final byte[] MAGIC = {'H', 'U', 'F', '1'};

    static String compress(File in, File outFile) throws Exception {
        long start = System.nanoTime();
        byte[] data = Files.readAllBytes(in.toPath());
        HuffmanCoder h = new HuffmanCoder(data);
        long[] freq = h.getFreq();

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(outFile)))) {

            // header
            out.write(MAGIC);
            out.writeLong(data.length);

            int count = 0;
            for (long f : freq) if (f > 0) count++;
            out.writeShort(count);
            for (int i = 0; i < 256; i++) {
                if (freq[i] > 0) {
                    out.writeByte(i);
                    out.writeLong(freq[i]);
                }
            }

            // bit hasil enkode
            BitWriter bw = new BitWriter(out);
            for (byte b : data) {
                bw.write(h.codeOf(b & 0xFF));
            }
            bw.flush();
        }
        return statistik("Kompres", in.length(), outFile.length(), start);
    }

    static String decompress(File in, File outFile) throws Exception {
        long start = System.nanoTime();

        try (DataInputStream din = new DataInputStream(
                new BufferedInputStream(new FileInputStream(in)));
             BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream(outFile))) {

            // header
            byte[] magic = new byte[4];
            din.readFully(magic);
            if (!Arrays.equals(magic, MAGIC)) {
                throw new IOException("Bukan file .huf yang valid");
            }
            long length = din.readLong();
            int count = din.readShort();
            long[] freq = new long[256];
            for (int i = 0; i < count; i++) {
                int sym = din.readUnsignedByte();
                freq[sym] = din.readLong();
            }

            // bangun pohon yang dari frekuensi di header
            HuffmanCoder h = new HuffmanCoder(freq);
            HuffmanCoder.Node root = h.getRoot();

            if (length > 0) {
                if (root.isLeaf()) {
                    // hanya satu jenis byte: tulis sebanyak panjang asli
                    for (long i = 0; i < length; i++) out.write(root.symbol);
                } else {
                    BitReader br = new BitReader(din);
                    HuffmanCoder.Node node = root;
                    long written = 0;
                    while (written < length) {
                        int bit = br.readBit();
                        if (bit == -1) throw new IOException("Data rusak: bit habis terlalu cepat");
                        node = (bit == 0) ? node.left : node.right;
                        if (node.isLeaf()) {
                            out.write(node.symbol);
                            written++;
                            node = root;
                        }
                    }
                }
            }
        }
        return statistik("Dekompres", in.length(), outFile.length(), start);
    }

    private static String statistik(String jenis, long awal, long akhir, long startNano) {
        long ms = (System.nanoTime() - startNano) / 1_000_000;
        String rasio = awal > 0 ? String.format("%.1f%%", akhir * 100.0 / awal) : "-";
        return jenis + ": " + awal + " -> " + akhir + " byte (" + rasio + "), " + ms + " ms";
    }
}