import java.io.IOException;
import java.io.InputStream;

class BitReader {
    private final InputStream in;
    private int cur = 0;
    private int n = 0;     // bit yang masih tersisa di cur

    BitReader(InputStream in) { this.in = in; }

    // mengembalikan 0 atau 1, atau -1 kalau data habis
    int readBit() throws IOException {
        if (n == 0) {
            cur = in.read();
            if (cur == -1) return -1;
            n = 8;
        }
        n--;
        return (cur >> n) & 1;
    }
}