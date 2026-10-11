# Mai — Monorepo

| Pasta | Conteúdo |
|---|---|
| `android/` | App Android nativo (Kotlin, Views, Gradle) — builda o APK |
| `backend/` | Backend (vazio por enquanto) |

## android

```powershell
cd android
.\gradlew assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Detalhes do app, keystore e publicação: ver `android/README.md`.
CI de build + assinatura: `.github/workflows/build-release.yml` (roda em `android/`).

## backend

Pasta reservada, vazia por enquanto.
