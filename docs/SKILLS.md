# Skills v2

Simpan dari plan setelah run COMPLETED, atau impor JSON plan strict: hasil selalu DRAFT. Skill berisi goal dan typed calls saja, bukan source code, key, scope aktif atau auto-executable payload. JSON export menampilkan isi plan yang dipilih; jangan menaruh rahasia dalam goal/value.

Alur: DRAFT → TESTING (muat) → jalankan Android harness dengan grant baru → verifier COMPLETED → REVIEW → pengguna APPROVED → pengguna ENABLED. Tool/package unsupported tidak bisa mendapat scope. Disabled harus melalui tes ulang. Gunakan selalu meminta scope baru; ENABLED bukan izin permanen.

Versi baru dimulai DRAFT dan tidak menimpa versi lama. Import/export/copy version/delete disediakan. Editing plan JSON bisa melalui ekspor, edit, lalu impor sebagai draft baru; belum ada visual parameterized skill editor atau recorder. Test hanya membuktikan field BYGA pada run itu; tidak ada klaim kompatibilitas ChatGPT/Flow/TikTok/Instagram.
