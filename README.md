# ChangeCut

ChangeCut adalah aplikasi Android video editor berbasis Jetpack Compose untuk editing video lokal, project lokal di device, dan alur kerja creator yang mengutamakan editing manual + bantuan AI.

## Gambaran Singkat

- Platform: Android
- Bahasa: Kotlin
- UI: Jetpack Compose + Material 3
- Dependency injection: Hilt
- Arsitektur: modular multi-module
- Target: video editor mobile dengan project lokal, tanpa cloud sync

## Fitur Utama

- Login, register, dan forgot password
- Home untuk recent project dan lanjut edit
- New project flow
- Editor screen dengan navigasi ke fitur editing
- Text, subtitle, audio, effect, transition, dan setting editor
- AI helper untuk content dan editing workflow
- Export hasil edit ke galeri

## Stack Teknis

- `compileSdk`: 36
- `minSdk`: 26
- `targetSdk`: 35
- Kotlin `2.0.21`
- Jetpack Compose
- Navigation Compose
- Hilt
- Kotlin Serialization

## Struktur Modul

- `app` - entry point aplikasi
- `common` - komponen dan utilitas bersama
- `core` - logic inti editor
- `data` - layer data
- `domain` - use case dan kontrak bisnis
- `feature-auth` - autentikasi
- `feature-home` - dashboard dan project lokal
- `feature-editor` - editor utama
- `feature-export` - flow export

## Prasyarat

- Android Studio terbaru
- JDK 17
- Android SDK sesuai konfigurasi project

## Setup Lokal

```bash
./gradlew assembleDebug
```

Kalau ingin menjalankan dari Android Studio:

1. Buka root folder repo `changecut-app`
2. Sync Gradle
3. Jalankan modul `app`

## Build

```bash
./gradlew build
```

Untuk debug APK:

```bash
./gradlew assembleDebug
```

## Struktur Project

Project ini memakai pendekatan modular agar fitur editor bisa dipisah per domain:

- `app` untuk bootstrap UI dan navigation utama
- `domain` untuk use case
- `data` untuk implementasi sumber data
- `core` untuk logika editing yang dipakai lintas fitur
- `feature-*` untuk layar dan flow per fitur

## Catatan Produk

- Project disimpan lokal di device
- Fokus utama adalah video editor mobile
- Semua fitur pro diposisikan sebagai bagian dari aplikasi, bukan sistem monetisasi
- AI dipakai untuk membantu editing, bukan menggantikan kontrol user

## Pengembangan

Jika menambah fitur baru, ikuti struktur modul yang sudah ada dan lanjutkan pattern yang dipakai project ini. Hindari refactor besar tanpa kebutuhan yang jelas.
