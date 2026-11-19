import java.util.*;

class HuffmanCoderText {
    HashMap<Character, String> encoder;
    HashMap<String, Character> decoder;

    private class Node implements Comparable<Node> {
        Character data;
        int freq;
        Node left;
        Node right;

        public Node (Character data, int freq) {
            this.data = data;
            this.freq = freq;
            this.left = null;
            this.right = null;
        }

        @Override
        public int compareTo(Node otherNode) {
            return this.freq - otherNode.freq;
        }
    }

    public HuffmanCoderText (String feeder) throws Exception {
        HashMap<Character, Integer> fmap = new HashMap<>();

        for (int i = 0; i < feeder.length(); i++) {
            char c = feeder.charAt(i);
            if(fmap.containsKey(c)) {
                int currVal = fmap.get(c);
                currVal += 1;
                fmap.put(c, currVal);
            } else {
                fmap.put(c, 1);
            }
        }

        Heap <Node> minHeap = new Heap<>();
        Set<Map.Entry<Character, Integer>> entrySet = fmap.entrySet();

        for(Map.Entry<Character, Integer> entry : entrySet) {
            Node node = new Node(entry.getKey(), entry.getValue());
            minHeap.insert(node);
        }

        while (minHeap.size() != 1) {
            Node first = minHeap.remove();
            Node second = minHeap.remove();

            Node newNode = new Node('\0', first.freq + second.freq);
            newNode.left = first;
            newNode.right = second;

            minHeap.insert(newNode);
        }

        Node root = minHeap.remove();

        this.encoder = new HashMap<>();
        this.decoder = new HashMap<>();

        this.initEncoderDecoder(root, "");
    }

    private void initEncoderDecoder(Node node, String currOutput) {
        if (node == null) return ;
        if (node.left == null && node.right == null) {
            this.encoder.put(node.data, currOutput);
            this.decoder.put(currOutput, node.data);
        }
        initEncoderDecoder(node.left, currOutput + "0");
        initEncoderDecoder(node.right, currOutput + "1");
    }

    public String encode (String source) {
        String ans = "";

        for (int i = 0; i < source.length(); i++) {
            ans = ans + encoder.get(source.charAt(i));
        }

        return ans;
    }

    public String decode (String codedString) {
        String key= "";
        String ans = "";

        for (int i = 0; i < codedString.length(); i++) {
            key = key + codedString.charAt(i);

            if (decoder.containsKey(key)) {
                ans = ans + decoder.get(key);
                key="";
            }
        }

        return ans;
    }
}