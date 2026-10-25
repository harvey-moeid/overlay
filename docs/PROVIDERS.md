# AI providers & model picker — 0.3.0

Provider lama dipertahankan. Tambahan: Cohere, Hugging Face Inference Providers, Mistral, NVIDIA NIM. Tidak ada backend tambahan; request HTTPS langsung dari APK. Metadata non-secret Room tetap memakai type/url/model; key tetap AES-GCM Android Keystore. Nama enum lama dipertahankan, konfigurasi tersimpan tidak di-reset.

| Provider | Base URL bawaan | Text API | Discovery model |
|---|---|---|---|
| Cohere | https://api.cohere.com | POST /v2/chat, Bearer token; response message.content[].text | GET /v1/models?endpoint=chat&page_size=1000 |
| Hugging Face | https://router.huggingface.co/v1 | POST /chat/completions; HF token dengan akses Inference Providers | GET /models |
| Mistral | https://api.mistral.ai/v1 | POST /chat/completions; Bearer API key | GET /models |
| NVIDIA NIM | https://integrate.api.nvidia.com/v1 | POST /chat/completions; Bearer API key | GET /models |

OpenAI/OpenRouter/Groq/custom mengikuti OpenAI-compatible; Anthropic tetap Messages dan Gemini generateContent. Katalog model semua provider bisa dipilih dari dropdown, difilter, atau ditulis manual. Saran bawaan bukan klaim kuota/akses/compatibility. Daftar live bisa berisi model non-chat; gunakan model text/chat yang sesuai. Untuk Gemini, daftar disaring ke generateContent; Cohere memakai filter endpoint=chat. Respons pagination ditandai parsial, ID manual tetap tersedia; discovery tidak berjalan terus-menerus.

## Cara pakai

1. AI Providers → pilih provider; endpoint otomatis terisi. Key/model dari provider sebelumnya tidak dibawa otomatis ke provider lain.
2. Isi key/token milik provider itu. Tekan **Ambil model dari API dengan key ini** untuk meminta daftar aktual, atau pilih saran dan edit ID manual. Pengambilan daftar tidak mengirim prompt; health test tetap tindakan terpisah dan dapat berbiaya.
3. Pilih model, lalu **Simpan provider terenkripsi**. Key dikosongkan dari form setelah simpan; tidak ditampilkan pada list.
4. Tekan **Kirim test koneksi** pada provider tersimpan untuk mengirim prompt kecil. Berhasil discovery bukan bukti inference akan berhasil: gated access, kredit/kuota, rate-limit dan model lifecycle dapat berbeda.
5. Agent planner otomatis menggunakan adapter provider yang tersimpan, tanpa penambahan hak akses agent atau upload layar.

HF: token harus memiliki izin Inference Providers dan akses gated model bila diperlukan; ID dapat memakai suffix :provider sesuai dokumentasi routing HF. NVIDIA: gunakan NVIDIA API Catalog key untuk hosted endpoint, bukan credential layanan lain. Mistral: gunakan API key console Mistral. Cohere: gunakan Cohere API key, bukan OpenAI key. Jangan simpan key di URL, source atau CI.

HTTPS tanpa credentials/query/fragment diwajibkan untuk base URL. Auth memakai header, redirect dan transport retry dimatikan, timeout 30 detik, respons <=1MiB. API errors hanya kode, tanpa body/key ke log. Unit tests memakai fake keys dan intercepted mock responses, bukan provider berbayar.

## Sumber kontrak dan saran

- Cohere SDK resmi Chat v2: https://github.com/cohere-ai/cohere-python/blob/main/src/cohere/v2/client.py
- Cohere model listing: https://github.com/cohere-ai/cohere-python/blob/main/src/cohere/models/client.py
- HF OpenAI-compatible routing: https://huggingface.co/docs/inference-providers/tasks/chat-completion
- HF model catalog (dibaca publik saat pengembangan): https://router.huggingface.co/v1/models
- Mistral chat API: https://docs.mistral.ai/api/endpoint/chat
- Mistral models: https://docs.mistral.ai/getting-started/models/models_overview/
- NVIDIA model catalog (dibaca publik saat pengembangan): https://integrate.api.nvidia.com/v1/models
- NVIDIA API reference: https://docs.api.nvidia.com/nim/reference/llm-apis

Halaman docs Cohere/NVIDIA menolak akses dari cloud ini; kontrak Cohere diperiksa melalui SDK resmi dan katalog HF/NVIDIA melalui endpoint publik. Tidak ada panggilan inference dengan credential nyata diuji di cloud. Ketersediaan model harus diuji pengguna dengan akun/runtime masing-masing.
