# CloneMail

Bank alias Gmail + autofill otomatis. v1+v2.

**Cara kerja:** Gmail plus-addressing — `namalo+situs@gmail.com` terkirim ke inbox Gmail dasar Anda.
Bukan akun Google terpisah. "Sign in with Google" tetap memakai akun dasar. Beberapa situs menolak
alamat ber-tanda `+`. Alias tidak melewati verifikasi nomor HP.

**Privasi:** 100% offline. Tidak menyimpan password apa pun. Semua data di HP Anda saja.

## Fitur
- **Bases** — daftar email dasar Gmail (validasi `@gmail.com`), tambah/hapus.
- **Generator** — pola: nama situs / situs+4 acak / situs+tanggal / tag custom; pratinjau; simpan + salin sekali ketuk.
- **Bank** — cari, filter status & base, salin, bagikan, arsip, hapus, tandai spam (+1 hit).
- **Backup** — export JSON/CSV ke Download, import JSON.
- **Autofill (v2)** — `CloneMailAutofillService` menyarankan alias pada kolom email di aplikasi/browser
  lain setelah diaktifkan sekali di Pengaturan sistem.

## Build
GitHub Actions → `assembleDebug` → rolling release `latest` (`CloneMail-vX.Y.Z.apk`).
Debug-signed — untuk install pribadi.
