# Sistem Informasi Manajemen Klinik Pratama

Java 17, Swing, PostgreSQL (JDBC), Maven. Arsitektur MVC + DAO.

## 1. Menyiapkan database (sekali saja)

```bash
# sebagai superuser postgres
psql -U postgres -f sql/00_buat_database.sql
# sebagai klinik_user
psql -U klinik_user -d db_klinik_pratama -f sql/schema.sql
psql -U klinik_user -d db_klinik_pratama -f sql/seed.sql
```

Mengulang dari nol: jalankan `sql/reset.sql`, lalu `schema.sql` dan `seed.sql` lagi.

Salin `db.properties.example` menjadi `db.properties` di folder utama proyek, lalu sesuaikan
`db.url`, `db.user`, `db.password`. Berkas ini tidak ikut di-commit.

## 2. Menjalankan

```bash
mvn -q compile exec:java      # atau run.bat di Windows
```

Akun contoh (dari seed.sql):

| Username | Password    | Role    |
|----------|-------------|---------|
| admin    | admin123    | ADMIN   |
| petugas  | petugas123  | PETUGAS |
| dokter   | dokter123   | DOKTER  |
| nonaktif | petugas123  | PETUGAS (akun nonaktif, login ditolak sesuai BR-01) |

## 3. Pengujian

```bash
test.bat                                             # aturan bisnis pada model, tanpa database
psql -U klinik_user -d db_klinik_pratama -f sql/uji_aturan_bisnis.sql   # aturan yang dijaga database
```

`uji_aturan_bisnis.sql` berjalan dalam satu transaksi lalu di-ROLLBACK sehingga data tidak berubah.

## 4. Alur pelayanan (Anggota 3)

1. **Pemeriksaan**: petugas mendaftarkan kunjungan (nomor antrean poli dibuat otomatis per dokter per hari). Antrean dapat dipanggil; setelah 3 kali panggilan kunjungan boleh dibatalkan dengan alasan.
2. **Rekam Medis**: tiga tahap berurutan. TTV (petugas) -> pemeriksaan dokter -> farmasi (petugas). Status kunjungan naik otomatis: TERDAFTAR -> TTV_SELESAI -> DIPERIKSA -> SELESAI.
3. **Pembayaran**: hanya kunjungan SELESAI yang belum dibayar. Total dihitung polimorfik (`LayananUmum` / `LayananSpesialis`). Pembayaran LUNAS terkunci.

Aturan bisnis dijaga dua lapis: di model/controller Java, dan di trigger serta constraint PostgreSQL.
