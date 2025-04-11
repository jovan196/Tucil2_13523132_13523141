# Tugas Kecil 2 IF2211
# Quadtree Image Compressor

Oleh:
13523132 - Jonathan Levi
13523141 - Jovandra Otniel P. S.

## a. Penjelasan Singkat Program
Program ini mengimplementasikan kompresi gambar berbasis **Quadtree** menggunakan algoritma divide and conquer. Program bekerja dengan membagi gambar menjadi blok-blok yang lebih kecil (kuadran) secara rekursif, kemudian menentukan apakah sebuah blok cukup homogen berdasarkan perhitungan error (misalnya, Variance, Mean Absolute Deviation, atau metode lainnya). Blok yang dianggap homogen (atau sudah mencapai ukuran minimum) direpresentasikan sebagai node daun (leaf), dan area tersebut akan diisi dengan warna rata-rata. Hasilnya adalah gambar yang telah terkompresi secara lossy, dengan file output yang berukuran lebih kecil. Program ini juga (opsional) membuat animasi GIF yang menampilkan proses pembentukan Quadtree secara progresif.

## b. Requirement Program dan Instalasi

### Requirement:
- Java Development Kit (JDK) – Versi 23 atau lebih baru.

### Instalasi:
1. Pastikan Java sudah terinstall dan prompt ```javac``` dan ```java``` bisa diakses melalui terminal.
2. Jika belum terunduh, clone repository program ini ke dalam folder local (di dalam PC).
```bash
git clone https://github.com/jovan196/Tucil2_13523132_13523141.git
```
3. Buka folder repository program ini
```bash
cd Tucil2_13523132_13523141
```

## c. Cara Compile dan Run Program
1. Pada terminal yang berada di root folder repository, buka (```cd```) folder ```src```
```bash
cd src
```
2. Compile seluruh file .java ke dalam folder ```bin```
```bash
javac *.java -d ../bin
```
3. Buka folder ```test``` untuk mempermudah input dan output file gambar ketika sudah membuka program
```bash
cd ../test
```
4. Run ```Main.class``` yang berada di folder ```bin```
```bash
java -cp ../bin Main
```
5. Masukkan nama file input (gambar) yang berada di folder ```test```. Output akan disimpan pada folder yang sama.