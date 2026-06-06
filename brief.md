# FULL PROMPT PROJECT: AI VIDEO EDITOR ANDROID SEPERTI CAPCUT PRO, SEMUA FITUR TERBUKA, TANPA MONETISASI, TANPA CLOUD

Saya ingin kamu membantu saya membangun aplikasi Android video editor yang konsepnya mirip aplikasi video editor profesional seperti CapCut, tetapi dengan identitas sendiri dan ditambahkan fitur AI yang lebih kuat.

Aplikasi ini bukan untuk dijual dengan sistem subscription, bukan freemium, bukan pakai credit, dan bukan memakai sistem cloud project. Semua fitur langsung tersedia setelah user login.

Target utama aplikasi ini adalah:

* Android mobile video editor
* Semua fitur pro langsung terbuka
* Hanya butuh login/register
* Project disimpan lokal di device
* Tidak ada monetisasi
* Tidak ada subscription
* Tidak ada watermark free/pro
* Tidak ada cloud sync
* Tidak ada team workspace
* Tidak ada payment
* Ditambahkan AI untuk membantu editing otomatis
* User tetap bisa edit manual seperti editor video biasa
* Style UI modern, profesional, clean, dan mudah digunakan

Saya sudah memiliki contoh codebase. Jadi sebelum membuat atau mengubah fitur, kamu harus membaca dan memahami codebase saya terlebih dahulu. Ikuti gaya code yang sudah ada, struktur folder, naming, pattern component, state management, dan cara project ini bekerja. Jangan langsung mengganti seluruh arsitektur tanpa alasan kuat.

## Tujuan Utama Aplikasi

Aplikasi ini harus menjadi video editor Android full-featured seperti aplikasi editor video profesional, dengan tambahan AI.

Workflow utama aplikasi:

1. User login
2. User membuat project baru
3. User memilih video/foto/audio dari galeri atau device
4. User masuk ke editor
5. User bisa mengedit manual
6. User bisa menggunakan AI untuk auto edit
7. AI bisa memilih momen terbaik, membuat draft timeline, subtitle, caption, dan rekomendasi style
8. User bisa mengubah hasil AI secara manual
9. User export video tanpa watermark
10. Video disimpan ke galeri atau dibagikan ke platform lain

## Batasan Penting

Aplikasi ini TIDAK membutuhkan:

* Monetisasi
* Subscription
* Free/pro plan
* AI credit
* Payment gateway
* Watermark karena user free
* Cloud project
* Cloud storage
* Cloud sync
* Team workspace
* Agency dashboard
* Admin payment dashboard
* Marketplace template
* Fitur pembelian dalam aplikasi

Aplikasi hanya membutuhkan:

* Login/register
* Semua fitur terbuka setelah login
* Project lokal
* Asset lokal
* Export lokal
* AI processing sesuai kebutuhan aplikasi

# CODEBASE REFERENCE

Saya sudah memiliki contoh codebase / base project yang harus dijadikan referensi utama pada project D:\Work\Pribadi\AndroidProject\shopme-app

Sebelum membuat fitur baru, kamu wajib:

1. Membaca dan memahami struktur codebase saya terlebih dahulu.
2. Mengikuti gaya coding yang sudah ada.
3. Mengikuti struktur folder yang sudah digunakan.
4. Mengikuti pola penamaan file, component, screen, service, hook, state, dan utility yang sudah ada.
5. Mengikuti cara project ini mengatur navigation, state management, UI component, API/service, storage, dan logic editor.
6. Jangan mengganti arsitektur utama tanpa alasan yang kuat.
7. Jangan membuat struktur baru yang bertabrakan dengan struktur existing.
8. Jika ada bagian yang kurang rapi, beri saran refactor dulu sebelum mengubah besar-besaran.
9. Jika ingin menambahkan fitur, sesuaikan dengan pattern yang sudah ada di codebase.
10. Jika ada fitur existing yang mirip, lanjutkan dari fitur tersebut, jangan membuat ulang dari nol.

Codebase saya harus dianggap sebagai sumber utama untuk:

* Style coding
* Struktur project
* UI pattern
* Component pattern
* Navigation flow
* State management
* Naming convention
* File organization
* Existing feature logic
* Existing editor workflow
* Existing design system

Jika ada konflik antara prompt fitur dan struktur codebase, jangan langsung memaksakan fitur baru. Analisa dulu cara terbaik agar fitur baru bisa masuk tanpa merusak project.

Output pertama yang saya inginkan setelah kamu membaca codebase:

1. Ringkasan struktur project
2. Teknologi/framework yang digunakan
3. Fitur yang sudah ada
4. Fitur yang belum ada
5. Bagian code yang sudah bagus
6. Bagian code yang perlu dirapikan
7. Rekomendasi folder/file yang akan digunakan untuk pengembangan berikutnya
8. Roadmap implementasi berdasarkan kondisi codebase saat ini

## Prinsip Pengembangan

Ikuti prinsip berikut:

1. Jangan membuat clone 1:1 dari CapCut secara brand, nama, asset, icon, template, atau desain yang melanggar hak cipta.
2. Buat aplikasi dengan inspirasi fitur editor video modern, tetapi tetap punya identitas sendiri.
3. Fokus pada pengalaman user yang simple, cepat, dan profesional.
4. AI harus membantu user, bukan menggantikan kontrol user.
5. Setiap hasil AI harus bisa diedit ulang oleh user.
6. Semua fitur pro langsung aktif.
7. Project harus bisa berjalan lokal di device.
8. Jika ada fitur yang terlalu berat untuk device, buat arsitektur yang tetap aman dan scalable.
9. Ikuti gaya codebase yang sudah ada.
10. Jangan refactor besar tanpa menjelaskan alasan dan manfaatnya.

# FITUR BESAR APLIKASI

## 1. Authentication / Login

Fitur:

* Register user
* Login user
* Forgot password
* Logout
* Profile sederhana
* Semua fitur langsung terbuka setelah login
* Tidak ada role free/pro
* Tidak ada subscription
* Tidak ada payment
* Tidak ada credit system

Tujuan:

User harus login agar bisa memakai aplikasi, tetapi setelah login semua fitur langsung tersedia.

## 2. Home & Local Project

Fitur:

* Home screen
* Tombol New Project
* Recent Projects
* Continue Editing
* Rename Project
* Duplicate Project
* Delete Project
* Project draft lokal
* Project history lokal
* Thumbnail project
* Informasi durasi project
* Informasi tanggal terakhir diedit

Ketentuan:

* Project tidak disimpan di cloud
* Project disimpan lokal di device
* Harus ada sistem autosave lokal
* User bisa melanjutkan project sebelumnya

## 3. Media Import

Fitur:

* Import video dari galeri
* Import foto dari galeri
* Import audio dari device
* Import multiple videos
* Preview media sebelum dipilih
* Sort media
* Select multiple files
* Basic media library lokal
* Validasi format media
* Warning jika format tidak didukung

Media yang didukung:

* Video
* Foto
* Audio
* Font custom
* Sticker custom
* Template lokal

## 4. Video Editor Core

Fitur utama:

* Preview video
* Timeline editor
* Trim video
* Cut video
* Split video
* Merge video
* Reorder clip
* Duplicate clip
* Delete clip
* Crop video
* Resize video
* Rotate video
* Flip video
* Reverse video
* Freeze frame
* Speed control
* Speed ramping
* Fit / Fill / Crop
* Background blur
* Background color

Tujuan:

Editor dasar harus kuat sebelum fitur AI dibuat terlalu kompleks.

## 5. Timeline & Layer System

Fitur:

* Timeline seperti editor video modern
* Multi-layer video
* Layer audio
* Layer text
* Layer sticker
* Layer effect
* Layer overlay
* Drag & drop clip
* Drag & drop layer
* Zoom in timeline
* Zoom out timeline
* Snap to clip
* Undo
* Redo
* Lock layer
* Hide/show layer
* Mute layer
* Reorder layer
* Timeline playhead
* Timeline scrubber
* Clip duration indicator
* Clip thumbnail preview

Layer yang dibutuhkan:

* Main video layer
* Overlay video layer
* Image layer
* Audio layer
* Text layer
* Subtitle layer
* Sticker layer
* Effect layer
* Adjustment layer

## 6. Canvas & Format

Fitur:

* Format 9:16 untuk TikTok/Reels/Shorts
* Format 16:9 untuk YouTube
* Format 1:1 untuk Instagram Feed
* Format 4:5 untuk social media
* Custom canvas size
* Background color
* Background blur
* Fit
* Fill
* Crop
* Safe area guide
* Grid guide
* Canvas preview
* Rotate canvas
* Resize canvas

Tujuan:

User bisa membuat video untuk berbagai platform social media.

## 7. Text & Subtitle

Fitur text:

* Add text
* Edit text
* Edit font
* Edit warna
* Edit ukuran
* Edit position
* Edit alignment
* Text animation
* Text template
* Text shadow
* Text stroke
* Text background
* Text opacity
* Text duration
* Text keyframe

Fitur subtitle:

* Subtitle manual
* Auto subtitle
* Edit subtitle per kalimat
* Subtitle style
* Subtitle animation
* Karaoke subtitle
* Caption viral style
* Translate subtitle
* Subtitle timing editor
* Subtitle position editor
* Subtitle export burned-in

Tujuan:

User bisa membuat subtitle modern seperti video TikTok/Reels/Shorts.

## 8. Audio & Music

Fitur:

* Tambah musik
* Tambah sound effect
* Tambah voice over
* Record voice over
* Extract audio dari video
* Trim audio
* Split audio
* Delete audio
* Duplicate audio
* Volume control
* Fade in audio
* Fade out audio
* Mute audio
* Audio speed
* Beat detection
* Auto beat sync
* Noise reduction
* Voice changer
* Audio waveform
* Audio sync with video

Tujuan:

Editor audio harus cukup kuat untuk kebutuhan creator short-form video.

## 9. Effects & Filters

Fitur:

* Video filters
* Visual effects
* Glitch effect
* Blur effect
* Flash effect
* Shake effect
* Cinematic effect
* Beauty effect
* Face effect
* Body effect
* Motion blur
* Color filter
* LUT / color preset
* Vignette
* Sharpen
* Grain
* Glow
* Light leak
* Camera effect

Ketentuan:

* Effect harus bisa ditambahkan ke clip atau layer
* Effect harus bisa diatur durasinya
* Effect harus bisa dihapus
* Effect harus bisa di-preview

## 10. Transitions

Fitur:

* Basic transition
* Fade transition
* Slide transition
* Zoom transition
* Spin transition
* Flash transition
* Smooth transition
* Camera movement transition
* Transition template
* AI recommended transition

Ketentuan:

* Transition bisa dipasang antar clip
* Transition bisa diatur durasinya
* Transition bisa di-preview
* User bisa mengganti transition dengan mudah

## 11. Pro Editing Features

Fitur pro:

* Keyframe animation
* Chroma key / green screen
* Background remover
* Object remover
* Motion tracking
* Stabilizer
* Masking
* Blending mode
* Adjustment layer
* Color adjustment
* Speed ramping
* Multi-track editing
* Layer opacity
* Layer transform
* Layer rotation
* Layer scale
* Layer position
* Layer animation

Tujuan:

Semua fitur pro tersedia langsung tanpa unlock atau payment.

## 12. Templates & Presets

Fitur:

* TikTok template
* Instagram Reels template
* YouTube Shorts template
* Vlog template
* Cinematic template
* Product video template
* Motivational template
* News/faceless template
* Football/highlight template
* Custom template
* Save own template
* Preset text style
* Preset subtitle style
* Preset color grading
* Preset transition
* Preset effect
* Preset export setting

Ketentuan:

* Template disimpan lokal
* Tidak ada template premium
* Semua template langsung tersedia
* User bisa membuat dan menyimpan template sendiri

## 13. AI Auto Edit

Ini adalah fitur pembeda utama aplikasi.

Fitur:

* AI pilih video terbaik
* AI pilih momen terbaik
* AI buang bagian jelek
* AI buang bagian kosong
* AI buang bagian terlalu gelap
* AI buang bagian blur
* AI buang bagian terlalu goyang
* AI auto cut
* AI susun timeline otomatis
* AI pilih opening
* AI pilih isi utama
* AI pilih closing
* AI buat draft video otomatis
* AI buat beberapa versi video
* AI rekomendasi durasi terbaik
* AI rekomendasi bagian yang paling menarik
* AI highlight detection
* AI scene detection

Workflow AI Auto Edit:

1. User memilih satu atau banyak video
2. User memilih tujuan video
3. AI menganalisa video
4. AI memberi score pada clip
5. AI memilih momen terbaik
6. AI menyusun draft timeline
7. User melihat preview
8. User bisa menerima hasil AI atau mengedit manual
9. User export video

Pilihan tujuan video:

* TikTok
* Reels
* Shorts
* Vlog
* Product video
* Cinematic
* Motivational
* News/faceless
* Football/highlight
* Custom

## 14. AI Content Assistant

Fitur:

* AI buat judul video
* AI buat caption
* AI buat hashtag
* AI buat script
* AI rewrite script
* AI buat hook
* AI buat CTA
* AI rekomendasi gaya video
* AI rekomendasi opening
* AI rekomendasi teks overlay
* AI rekomendasi struktur video
* AI buat beberapa versi konten
* AI rekomendasi durasi video
* AI rekomendasi platform terbaik
* AI rekomendasi subtitle style

Tujuan:

AI membantu creator membuat video yang lebih menarik, bukan hanya mengedit visual.

## 15. AI Visual Features

Fitur:

* AI background remover
* AI object remover
* AI auto crop
* AI auto zoom
* AI face enhancement
* AI scene detection
* AI B-roll suggestion
* AI image generator
* AI video generator
* AI style transfer
* AI thumbnail frame suggestion
* AI lighting correction
* AI blur detection
* AI best frame picker
* AI visual quality score

Ketentuan:

* Setiap hasil AI harus bisa diedit user
* AI tidak boleh langsung memaksa perubahan tanpa preview
* User harus bisa undo hasil AI

## 16. AI Audio Features

Fitur:

* AI auto subtitle
* AI speech-to-text
* AI text-to-speech
* AI voice over
* AI voice changer
* AI noise remover
* AI music recommendation
* AI sound effect recommendation
* AI beat sync
* AI dubbing
* AI translate voice/subtitle
* AI audio cleanup
* AI detect silence
* AI remove silence
* AI normalize volume

Tujuan:

AI membantu mempercepat pekerjaan audio dan subtitle.

## 17. Export & Share

Fitur:

* Export 720p
* Export 1080p
* Export 2K
* Export 4K
* Export 24fps
* Export 30fps
* Export 60fps
* Export 9:16
* Export 16:9
* Export 1:1
* Export 4:5
* Export tanpa watermark
* Save ke galeri
* Share ke TikTok
* Share ke Instagram
* Share ke YouTube Shorts
* Share ke WhatsApp
* Share file video
* Export progress
* Export error handling
* Export history lokal

Ketentuan:

* Tidak boleh ada watermark karena tidak ada free/pro plan
* Semua kualitas export tersedia
* User bisa memilih resolusi dan FPS

## 18. Local Asset Management

Fitur:

* Asset tersimpan lokal
* Font lokal
* Music lokal
* Effect lokal
* Template lokal
* Sticker lokal
* Recently used assets
* Favorite assets
* Import custom font
* Import custom music
* Import custom sticker
* Import custom template
* Delete local asset
* Manage local asset
* Clear unused asset

Tujuan:

Semua asset dan project dikelola lokal tanpa cloud.

## 19. App Settings

Fitur:

* Bahasa aplikasi
* Dark mode
* Light mode
* Default export quality
* Default canvas ratio
* Default subtitle style
* Default save location
* Clear cache
* Manage local storage
* Privacy settings
* App version info
* Performance mode
* Render quality setting
* Auto save setting

Tujuan:

User bisa mengatur pengalaman aplikasi sesuai kebutuhan.

## 20. Local Safety & Copyright Helper

Fitur:

* Copyright warning untuk musik/asset
* Warning jika audio kemungkinan bermasalah
* Warning jika video terlalu blur
* Warning jika video terlalu gelap
* Warning jika video terlalu panjang
* Warning jika storage device hampir penuh
* Warning jika export gagal
* Warning jika format tidak didukung
* Warning jika resolusi terlalu berat untuk device
* Warning jika project corrupt
* Warning jika file sumber hilang dari device

Catatan:

Fitur ini hanya helper lokal, bukan sistem legal final. Aplikasi cukup memberi peringatan agar user sadar ada kemungkinan masalah.

# HALAMAN / SCREEN YANG DIBUTUHKAN

## 1. Splash Screen

* Logo aplikasi
* Loading session
* Redirect ke login atau home

## 2. Login Screen

* Email/password login
* Register
* Forgot password

## 3. Home Screen

* New project
* Recent projects
* Continue editing
* Settings
* Profile

## 4. New Project Screen

* Pilih media
* Pilih format video
* Pilih tujuan video
* Pilih manual edit atau AI auto edit

## 5. Media Picker Screen

* Galeri video
* Galeri foto
* Audio picker
* Multi select
* Preview media
* Import selected media

## 6. AI Analysis Screen

* Progress analyzing
* Daftar clip yang direkomendasikan
* Score setiap clip
* Pilihan generate draft
* Pilihan regenerate

## 7. Editor Screen

* Video preview
* Timeline
* Tool panel
* Layer panel
* Play/pause
* Undo/redo
* Export button

## 8. Timeline Screen / Component

* Multi-layer timeline
* Clip thumbnails
* Audio waveform
* Drag & drop
* Zoom timeline
* Split/trim controls

## 9. Text Editor Screen

* Add text
* Edit style
* Edit animation
* Edit position
* Edit duration

## 10. Subtitle Screen

* Auto subtitle
* Manual subtitle
* Edit subtitle
* Subtitle style
* Translate subtitle

## 11. Audio Screen

* Add music
* Add voice over
* Extract audio
* Volume
* Fade
* Beat sync
* Noise removal

## 12. Effect Screen

* Filter list
* Effect list
* Preview effect
* Apply effect
* Remove effect

## 13. Transition Screen

* Transition list
* Preview transition
* Apply transition
* Set duration

## 14. Template Screen

* Template category
* Apply template
* Save custom template
* Manage template

## 15. Export Screen

* Choose resolution
* Choose FPS
* Choose format
* Export progress
* Save to gallery
* Share video

## 16. Settings Screen

* Language
* Theme
* Export defaults
* Storage
* Clear cache
* About app

# FLOW UTAMA APLIKASI

## Flow Manual Editing

1. User login
2. User klik New Project
3. User pilih video/foto/audio
4. User pilih canvas ratio
5. User masuk editor
6. User edit manual
7. User menambahkan text/audio/effect/transition
8. User export video
9. Video disimpan ke galeri

## Flow AI Auto Edit

1. User login
2. User klik New Project
3. User pilih beberapa video
4. User pilih tujuan video
5. User klik AI Auto Edit
6. AI menganalisa video
7. AI memilih momen terbaik
8. AI membuat draft timeline
9. User melihat preview
10. User bisa regenerate atau edit manual
11. User export video

## Flow AI Subtitle

1. User masuk editor
2. User pilih Auto Subtitle
3. AI transcribe audio
4. Subtitle muncul di timeline
5. User bisa edit subtitle
6. User pilih style subtitle
7. Subtitle masuk ke video saat export

## Flow AI Content Assistant

1. User selesai membuat video
2. User klik AI Content
3. AI membuat title, caption, hashtag, dan CTA
4. User bisa copy hasilnya
5. User bisa export/share video

# PRIORITAS PENGERJAAN

Kerjakan aplikasi ini secara bertahap.

## Phase 1: Audit Codebase

Tugas:

* Baca struktur project
* Pahami teknologi yang dipakai
* Cek fitur yang sudah ada
* Cek UI yang sudah ada
* Cek error atau kekurangan
* Ikuti gaya codebase yang ada

Output:

* Ringkasan kondisi project
* Daftar fitur yang sudah ada
* Daftar fitur yang belum ada
* Daftar masalah
* Rekomendasi urutan pengerjaan

## Phase 2: UI Foundation

Tugas:

* Rapikan desain dasar
* Buat flow screen utama
* Buat Home screen
* Buat New Project screen
* Buat Media Picker screen
* Buat Editor layout awal
* Buat Export screen

Output:

* UI terlihat modern dan profesional
* Navigation jelas
* User flow mudah dipahami

## Phase 3: Core Editor

Tugas:

* Import video
* Preview video
* Timeline sederhana
* Trim video
* Split video
* Reorder clip
* Merge clip
* Export video

Output:

* User sudah bisa membuat video sederhana dari beberapa clip

## Phase 4: Timeline & Layer

Tugas:

* Multi-layer timeline
* Text layer
* Audio layer
* Overlay layer
* Timeline zoom
* Undo/redo
* Drag & drop

Output:

* Editor mulai terasa seperti editor video profesional

## Phase 5: Text, Subtitle, Audio

Tugas:

* Add text
* Text style
* Subtitle manual
* Auto subtitle
* Add music
* Voice over
* Audio trim
* Volume control

Output:

* User bisa membuat video sosial media yang lengkap

## Phase 6: Effects, Filters, Transitions

Tugas:

* Filter
* Effects
* Transition
* Color adjustment
* Preview effect
* Apply/remove effect

Output:

* Video bisa dibuat lebih menarik secara visual

## Phase 7: Pro Features

Tugas:

* Keyframe
* Chroma key
* Background remover
* Motion tracking
* Stabilizer
* Masking
* Adjustment layer
* Speed ramping

Output:

* Fitur pro mulai lengkap

## Phase 8: AI Auto Edit

Tugas:

* AI analyze video
* AI score clip
* AI choose best moment
* AI remove bad/empty parts
* AI create draft timeline
* AI recommend opening/content/closing
* AI generate multiple versions

Output:

* User bisa memilih video dan AI membuat draft edit otomatis

## Phase 9: AI Content, Visual, Audio

Tugas:

* AI title
* AI caption
* AI hashtag
* AI hook
* AI subtitle
* AI TTS
* AI noise removal
* AI background remover
* AI object remover
* AI auto crop
* AI auto zoom

Output:

* Aplikasi menjadi AI-first video editor

## Phase 10: Polish, Performance, Export

Tugas:

* Optimasi performa
* Perbaiki render
* Perbaiki export
* Testing banyak device
* Error handling
* Storage cleanup
* UI polish
* Bug fixing

Output:

* Aplikasi siap digunakan dengan pengalaman yang stabil

# MVP PALING AWAL

Walaupun target akhirnya besar, MVP awal harus fokus pada fondasi.

MVP awal minimal harus punya:

* Login
* Home
* New project
* Import video
* Preview video
* Timeline sederhana
* Trim video
* Split video
* Reorder clip
* Add text
* Add music
* Export tanpa watermark
* Save ke galeri
* AI pilih momen terbaik
* AI buat draft timeline sederhana

Jangan langsung membuat semua fitur pro sebelum core editor stabil.

# ATURAN KERJA UNTUK AI/DEVELOPER

Saat membantu mengerjakan codebase saya, lakukan ini:

1. Baca codebase dulu.
2. Jangan membuat asumsi berlebihan.
3. Ikuti gaya coding yang sudah ada.
4. Jangan menghapus fitur yang sudah ada tanpa alasan.
5. Jika perlu refactor, jelaskan alasan dan dampaknya.
6. Kerjakan bertahap.
7. Buat perubahan yang jelas dan bisa diuji.
8. Setelah membuat perubahan, jelaskan file mana yang diubah.
9. Jelaskan fitur apa yang berhasil dibuat.
10. Jelaskan jika ada keterbatasan.
11. Jangan membuat sistem monetisasi.
12. Jangan membuat cloud sync.
13. Jangan membuat payment.
14. Jangan membuat free/pro plan.
15. Semua fitur harus dianggap aktif setelah login.
16. Prioritaskan editor core sebelum AI kompleks.
17. Semua hasil AI harus bisa diedit manual.
18. Jangan meniru nama, logo, asset, atau template CapCut secara langsung.
19. Buat UI modern dengan identitas sendiri.
20. Fokus pada aplikasi Android video editor pro + AI.
21. Codebase saya adalah referensi utama. Jangan membuat implementasi baru dari nol jika fitur serupa sudah ada di codebase. Lanjutkan, rapikan, dan kembangkan dari struktur yang sudah ada.

# OUTPUT YANG SAYA INGINKAN DARI KAMU

Setelah membaca codebase, berikan:

1. Audit kondisi project
2. Struktur fitur yang sudah ada
3. Struktur fitur yang belum ada
4. Masalah teknis yang ditemukan
5. Rekomendasi roadmap
6. Prioritas pengerjaan
7. Perubahan code yang perlu dilakukan
8. Implementasi bertahap
9. Penjelasan file yang diubah
10. Testing checklist

# RINGKASAN TARGET AKHIR

Target akhir aplikasi:

Aplikasi Android video editor profesional seperti CapCut-style, tetapi dengan identitas sendiri, semua fitur pro langsung terbuka setelah login, project tersimpan lokal di device, tanpa monetisasi, tanpa cloud, tanpa watermark, dan ditambahkan AI untuk auto edit, subtitle, visual, audio, serta content assistant.

Workflow utama:

User login → buat project → pilih media → edit manual atau AI auto edit → AI membuat draft → user bisa edit ulang → export video tanpa watermark → save/share.

Fokus utama:

* Video editor pro
* Timeline multi-layer
* Text dan subtitle
* Audio dan music
* Effects dan filters
* Transitions
* Pro editing tools
* Templates lokal
* AI auto edit
* AI content assistant
* AI visual tools
* AI audio tools
* Export lengkap
* Local project management
* No monetization
* No cloud
* All features unlocked after login
