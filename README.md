# Kompresi File dengan Kode Huffman

Program kompresi file sederhana memakai algoritma Huffman (Java).
Proyek mata kuliah Matematika Diskrit.

## Cara Kerja

Byte yang sering muncul diberi kode bit pendek, yang jarang muncul diberi kode bit panjang,
sehingga ukuran file jadi lebih kecil.

## Cara Menjalankan

Kompilasi (PowerShell):

```powershell
$files = Get-ChildItem -Path huffman -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -d bin $files
```

Jalankan GUI:

```
java -cp bin huffman.ui.App
```

Jalankan lewat CLI:

```
java -cp bin huffman.Main c <file_asli> <file.huf>      kompres
java -cp bin huffman.Main d <file.huf> <file_hasil>     dekompres
java -cp bin huffman.Main t <file_asli>                 uji round trip
```

## Contoh

```
java -cp bin huffman.Main t samples/contoh.txt
```