class HuffmanCoder {
    static class Node implements Comparable<Node> {
        int symbol;
        long freq;
        Node left, right;

        Node (int symbol, long freq) {
            this.symbol = symbol;
            this.freq = freq;
        }

        boolean isLeaf () { return left == null && right == null; }

        @Override
        public int compareTo(Node other) { return Long.compare(freq, other.freq); }

    }    

    private final long[] freq;
    private String codes[] = new String[256];
    private Node root;

    // Hitung Frequensi Tiap byte
    static long[] countFreq (byte[] data) {
        long[] f = new long[256];
        for (byte b : data) { 
            f[b & 0xFF]++; 
        }
        return f;
    }

    HuffmanCoder(byte[] data) throws Exception {
        this(countFreq(data));
    }

    HuffmanCoder(long[] freq) throws Exception {
        this.freq = freq.clone();
        Heap<Node> heap = new Heap<>();

        for (int i = 0; i < 256; i++) {
            if (freq[i] > 0) heap.insert(new Node(i, freq[i]));
        }  
        if (heap.size() == 0) return ; // Check jika kosong

        while (heap.size() > 1) {
            Node a = heap.remove();
            Node b = heap.remove();
            Node p = new Node(-1, a.freq + b.freq);
            p.left = a;
            p.right = b;
            heap.insert(p);
        }
        root = heap.remove();

        if(root.isLeaf()) codes[root.symbol] = "0";
        else buildCodes(root, "");
    }

    private void buildCodes(Node n, String prefix) {
        if (n.isLeaf()) { codes[n.symbol] = prefix; return; }
        buildCodes(n.left, prefix + "0");
        buildCodes(n.right, prefix + "1");
    }

    String codeOf(int symbol) { return codes[symbol]; }
    Node getRoot() { return root; }
    long[] getFreq () { return freq.clone();}
}
