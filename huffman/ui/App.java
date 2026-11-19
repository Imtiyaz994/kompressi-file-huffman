package huffman.ui;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import huffman.service.FileCompressor;
import huffman.service.Hasil;
import huffman.service.HufInspector;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class App extends JFrame {

    // ---------- komponen kiri ----------
    private final JTextField pathField = new JTextField();
    private final JButton btnPilih = new JButton("Pilih file...");
    private final JButton btnKompres = new JButton("Kompres");
    private final JButton btnDekompres = new JButton("Dekompres");
    private final JProgressBar bar = new JProgressBar();
    private final JTextArea log = new JTextArea();
    private File terpilih;

    // ---------- komponen kanan ----------
    private final DefaultTableModel ringkasanModel = new DefaultTableModel(
            new Object[]{"Item", "Nilai"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable ringkasanTable = new JTable(ringkasanModel);
    private final BarPanel barPanel = new BarPanel();

    private final DefaultTableModel kodeModel = new DefaultTableModel(
            new Object[]{"Byte", "Huruf", "Frekuensi", "Kode", "Panjang", "Total bit"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Class<?> getColumnClass(int c) {
            switch (c) {
                case 0: case 4: return Integer.class;
                case 2: case 5: return Long.class;
                default: return String.class;
            }
        }
    };
    private final JTable kodeTable = new JTable(kodeModel);
    private final JTextArea biner = new JTextArea();
    private final JTabbedPane tabs = new JTabbedPane();

    private TransferHandler dropHandler;

    public App() {
        super("Kompresi File dengan Kode Huffman");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        pathField.setEditable(false);
        log.setEditable(false);
        log.setLineWrap(true);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        log.setText("Tarik dan lepas file ke jendela ini, atau klik \"Pilih file...\".\n");
        biner.setEditable(false);
        biner.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        biner.setText("(Pilih atau hasilkan file .huf untuk melihat isi binernya.)");
        btnKompres.setEnabled(false);
        btnDekompres.setEnabled(false);

        // ----- panel kiri -----
        JPanel baris1 = new JPanel(new BorderLayout(6, 0));
        baris1.add(pathField, BorderLayout.CENTER);
        baris1.add(btnPilih, BorderLayout.EAST);

        JPanel baris2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        baris2.add(btnKompres);
        baris2.add(btnDekompres);

        JPanel kontrol = new JPanel(new GridLayout(2, 1, 0, 8));
        kontrol.add(baris1);
        kontrol.add(baris2);

        JPanel kiri = new JPanel(new BorderLayout(8, 8));
        kiri.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        kiri.add(kontrol, BorderLayout.NORTH);
        kiri.add(new JScrollPane(log), BorderLayout.CENTER);
        kiri.add(bar, BorderLayout.SOUTH);

        // ----- panel kanan -----
        ringkasanTable.setRowHeight(22);
        ringkasanTable.getColumnModel().getColumn(0).setPreferredWidth(230);
        ringkasanTable.getColumnModel().getColumn(1).setPreferredWidth(260);

        JPanel tabRingkasan = new JPanel(new BorderLayout(0, 8));
        tabRingkasan.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        tabRingkasan.add(new JScrollPane(ringkasanTable), BorderLayout.CENTER);
        tabRingkasan.add(barPanel, BorderLayout.SOUTH);

        kodeTable.setAutoCreateRowSorter(true);
        kodeTable.setRowHeight(22);

        tabs.addTab("Ringkasan", tabRingkasan);
        tabs.addTab("Tabel Kode", new JScrollPane(kodeTable));
        tabs.addTab("Biner", new JScrollPane(biner));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, kiri, tabs);
        split.setResizeWeight(0.38);
        split.setDividerLocation(400);
        add(split, BorderLayout.CENTER);

        // ----- aksi -----
        btnPilih.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                pilih(fc.getSelectedFile());
            }
        });
        btnKompres.addActionListener(e -> jalankan(true));
        btnDekompres.addActionListener(e -> jalankan(false));

        pasangDrop();

        setSize(1080, 620);
        setLocationRelativeTo(null);
        tampilkanKosong();
    }

    // ---------- drag and drop ----------
    private void pasangDrop() {
        dropHandler = new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport s) {
                return s.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport s) {
                if (!canImport(s)) return false;
                try {
                    List<?> files = (List<?>) s.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        pilih((File) files.get(0));
                        return true;
                    }
                } catch (Exception ex) {
                    log.append("Gagal membaca file yang dilepas: " + ex + "\n");
                }
                return false;
            }
        };
        JComponent[] semua = {
                (JComponent) getContentPane(), pathField, log, biner,
                ringkasanTable, kodeTable, tabs
        };
        for (JComponent c : semua) c.setTransferHandler(dropHandler);
    }

    // ---------- memilih file ----------
    private void pilih(File f) {
        if (!f.isFile()) {
            log.append("Itu bukan file: " + f + "\n");
            return;
        }
        terpilih = f;
        pathField.setText(f.getAbsolutePath());
        btnKompres.setEnabled(true);
        btnDekompres.setEnabled(true);
        log.append("Dipilih: " + f.getName() + " (" + f.length() + " byte)\n");

        if (f.getName().endsWith(".huf")) {
            refreshBiner(f);
        } else {
            biner.setText("(File ini bukan .huf. Hasilkan file .huf lewat tombol Kompres.)");
        }
    }

    private void refreshBiner(File f) {
        try {
            biner.setText(HufInspector.inspect(f, 64));   // 64 = jumlah byte data yang ditampilkan
        } catch (Exception ex) {
            biner.setText("Tidak bisa membaca sebagai .huf: " + ex);
        }
        biner.setCaretPosition(0);
    }

    // ---------- kompres / dekompres ----------
    private void jalankan(boolean kompres) {
        File in = terpilih;

        String saran;
        if (kompres) {
            saran = in.getPath() + ".huf";
        } else if (in.getName().endsWith(".huf")) {
            String dasar = in.getPath().substring(0, in.getPath().length() - 4);
            if (new File(dasar).exists()) {
                int titik = dasar.lastIndexOf('.');
                int pemisah = Math.max(dasar.lastIndexOf('\\'), dasar.lastIndexOf('/'));
                dasar = (titik > pemisah)
                        ? dasar.substring(0, titik) + "_balik" + dasar.substring(titik)
                        : dasar + "_balik";
            }
            saran = dasar;
        } else {
            saran = in.getPath() + ".out";
        }

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(saran));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File out = fc.getSelectedFile();

        if (out.equals(in)) {
            JOptionPane.showMessageDialog(this, "File tujuan tidak boleh sama dengan file asal.");
            return;
        }
        if (out.exists() && JOptionPane.showConfirmDialog(this,
                out.getName() + " sudah ada. Timpa?", "Konfirmasi",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        setBusy(true);
        new SwingWorker<Hasil, Void>() {
            @Override
            protected Hasil doInBackground() throws Exception {
                return kompres ? FileCompressor.compress(in, out)
                               : FileCompressor.decompress(in, out);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    Hasil r = get();
                    log.append(r + "\n");
                    tampilkan(r);
                    if (kompres) refreshBiner(out);
                    tabs.setSelectedIndex(0);
                } catch (ExecutionException ex) {
                    log.append("GAGAL: " + ex.getCause() + "\n");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void setBusy(boolean sibuk) {
        btnPilih.setEnabled(!sibuk);
        btnKompres.setEnabled(!sibuk && terpilih != null);
        btnDekompres.setEnabled(!sibuk && terpilih != null);
        bar.setIndeterminate(sibuk);
    }

    // ---------- menampilkan hasil di panel kanan ----------
    private void tampilkanKosong() {
        ringkasanModel.setRowCount(0);
        ringkasanModel.addRow(new Object[]{"Status", "Belum ada operasi. Kompres atau dekompres sebuah file."});
        kodeModel.setRowCount(0);
        barPanel.set("", 0, "", 0, false);
    }

    private static String ukuran(long b) {
        return String.format("%,d byte", b);
    }

    private void baris(String item, String nilai) {
        ringkasanModel.addRow(new Object[]{item, nilai});
    }

    private static String huruf(int i) {
        if (i >= 33 && i <= 126) return String.valueOf((char) i);
        switch (i) {
            case 32: return "spasi";
            case 10: return "\\n";
            case 13: return "\\r";
            case 9:  return "\\t";
            default: return "-";
        }
    }

    private void tampilkan(Hasil r) {
        boolean kompres = r.jenis.equals("Kompres");
        String namaAwal  = kompres ? "Ukuran asli" : "Ukuran file .huf";
        String namaAkhir = kompres ? "Ukuran hasil (.huf)" : "Ukuran hasil dekompresi";

        ringkasanModel.setRowCount(0);
        baris("Operasi", r.jenis);
        baris("Format file .huf", r.mode);
        baris(namaAwal, ukuran(r.awal));
        baris(namaAkhir, ukuran(r.akhir));
        if (kompres && r.awal > 0) {
            baris("Rasio (hasil / asli)", String.format("%.1f%%", r.akhir * 100.0 / r.awal));
            long hemat = r.awal - r.akhir;
            baris("Penghematan", hemat >= 0
                    ? String.format("%,d byte (%.1f%%)", hemat, hemat * 100.0 / r.awal)
                    : String.format("membengkak %,d byte", -hemat));
        }
        baris("Waktu proses", r.ms + " ms");

        if (r.freq != null && r.panjangAsli > 0) {
            baris("Jumlah jenis byte", String.valueOf(r.jumlahSimbol));
            baris("Header", r.headerByte + " byte");
            baris("Bit data (kode Huffman)", String.format("%,d bit", r.totalBit));
            baris("Bit tanpa kompresi (8 bit/byte)", String.format("%,d bit", r.panjangAsli * 8));
            double rata = r.rataKode();
            baris("Panjang kode rata-rata", String.format("%.3f bit/byte", rata));
            baris("Entropi sumber", String.format("%.3f bit/byte", r.entropi));
            baris("Efisiensi kode (entropi / rata-rata)",
                    (r.jumlahSimbol > 1 && rata > 0)
                            ? String.format("%.1f%%", r.entropi / rata * 100) : "-");
        }
        if (r.mentah) {
            baris("Catatan", kompres
                    ? "Huffman tidak menguntungkan, file disimpan apa adanya (HUF0)."
                    : "File berformat HUF0, disalin apa adanya.");
        } else {
            baris("Catatan", kompres ? "Dikompresi dengan kode Huffman (HUF1)."
                                     : "Dikembalikan dari kode Huffman (HUF1).");
        }

        // tabel kode
        kodeModel.setRowCount(0);
        if (r.freq != null && r.kode != null) {
            for (int i = 0; i < 256; i++) {
                if (r.freq[i] > 0 && r.kode[i] != null) {
                    kodeModel.addRow(new Object[]{
                            i, huruf(i), r.freq[i], r.kode[i],
                            r.kode[i].length(), r.freq[i] * r.kode[i].length()});
                }
            }
            kodeTable.getRowSorter().setSortKeys(List.of(
                    new RowSorter.SortKey(2, SortOrder.DESCENDING)));
        }

        // grafik batang
        barPanel.set(kompres ? "Asli" : ".huf", r.awal,
                     kompres ? "Hasil .huf" : "Hasil dekompresi", r.akhir, kompres);
    }

    // ---------- grafik batang sederhana ----------
    private static class BarPanel extends JPanel {
        private String l1 = "", l2 = "";
        private long v1, v2;
        private boolean warnaHasil;

        BarPanel() { setPreferredSize(new Dimension(400, 100)); }

        void set(String l1, long v1, String l2, long v2, boolean warnaHasil) {
            this.l1 = l1; this.v1 = v1;
            this.l2 = l2; this.v2 = v2;
            this.warnaHasil = warnaHasil;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            long maks = Math.max(Math.max(v1, v2), 1);
            int w = getWidth() - 20;
            Color warna2 = !warnaHasil ? new Color(0x2F6FD0)
                    : (v2 <= v1 ? new Color(0x2E9B4E) : new Color(0xD03A2F));
            String[] label = {l1 + ": " + String.format("%,d", v1) + " byte",
                              l2 + ": " + String.format("%,d", v2) + " byte"};
            long[] nilai = {v1, v2};
            Color[] warna = {new Color(0x888888), warna2};
            int y = 10;
            for (int i = 0; i < 2; i++) {
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(label[i], 10, y + 10);
                int lebar = (int) (w * (double) nilai[i] / maks);
                g2.setColor(warna[i]);
                g2.fillRoundRect(10, y + 16, Math.max(lebar, 2), 14, 6, 6);
                y += 42;
            }
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new App().setVisible(true));
    }
}