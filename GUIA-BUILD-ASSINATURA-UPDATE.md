# Guia Definitivo — Build, Assinatura e Auto-Update de Apps Android
*Todo o aprendizado do projeto Mai, escrito para valer para qualquer app.*

---

## 1. Assinatura: a identidade do app

### O conceito
O keystore (`.p12`/`.jks`) é a **identidade** do app. O Android só permite atualizar por cima se a assinatura for **idêntica**. Assinaturas diferentes = `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, sem exceção.

### Regras de ouro
1. **Um keystore por app, para sempre.** Perdeu = nunca mais atualiza por cima; terá que publicar com outro `applicationId`.
2. **Guarde 3 coisas com backup**: o arquivo `.p12`, a senha da store e a senha da chave (podem ser iguais).
3. **Debug vs Release nunca se misturam**: debug usa a chave universal do SDK; release usa a sua. Escolha um canal por dispositivo de teste.
4. **Nunca commite**: `.p12`, senhas e `signing.properties` ficam fora do git (só o `.example` é versionado).
5. **No CI, o keystore viaja como base64**: secret `KEYSTORE_BASE64` → decodificado no runner → usado → descartado. Linha única, sem quebras (`[Convert]::ToBase64String` no PowerShell).

### Como conferir se é a mesma chave
```powershell
apksigner verify --print-certs app.apk   # SHA-256 do certificado
keytool -list -v -keystore sua-chave.p12 # compara o SHA-256
```
DN e fingerprint iguais = mesma chave = update por cima garantido.

---

## 2. versionCode vs versionName

| Campo | O que é | Quem vê | Regra |
|---|---|---|---|
| `versionName` (`0.2.0`) | Nome bonito da versão | Usuário | Sobe a cada release pública |
| `versionCode` (inteiro) | Número interno de build | Só o Android | **Sempre crescente**, senão `INSTALL_FAILED_VERSION_DOWNGRADE` |

### Estratégias de versionCode
- **Run number do CI** (recomendado): `GITHUB_RUN_NUMBER` — simples, garantido crescente, zero esforço. Falhas também consomem número (explica os "buracos" 6→8→9).
- **Por data** (`yyyyMMddNN`): legível (`2026101101`), mas tem limite (99 builds/dia) e parsing extra. Use a **data só como campo informativo** (`"date": "11/10/2026"` no JSON), não como versionCode.
- **Local**: fallback fixo (ex: `2`). Builds locais nunca competem com o CI.

```kotlin
versionCode = providers.environmentVariable("GITHUB_RUN_NUMBER")
    .orNull?.toIntOrNull() ?: 2
```

---

## 3. CI: o robô que builda e assina

### Estrutura mínima (GitHub Actions, `ubuntu-latest`)
1. `setup-java` com **JDK 17 Temurin** (AGP 8.x não roda em 21+).
2. `setup-android@v4` com `packages: ''` — a v3 quebrava pedindo o pacote `tools`, removido pelo Google.
4. `local.properties` gerado no runner (`sdk.dir=$ANDROID_HOME`) — o local nunca vai pro git.
5. Keystore decodificado do secret para `app/build/`.
6. `./gradlew assembleRelease` com `KEY_ALIAS` + `KEYSTORE_PASSWORD` via env.
7. `apksigner verify` — prova que assinou.
8. Publica **Release** com tag `v<versionName>` + APK anexado (artifact expira em dias; Release é permanente).

### Armadilhas reais (todas aconteceram aqui)
- **`gradlew` com aspas aninhadas** (`'"-Xmx64m"'`): no bash do runner o java recebe as aspas como texto → `Could not find main class "-Xmx64m"`. Formato certo: `"-Xmx64m -Xms64m"`. No Windows passa por acidente — só quebra no Linux.
- **`--offline` na primeira execução**: o CI precisa baixar Gradle/AGP; offline falha em silêncio. Nunca use `--offline` para a primeira task do job.
- **Tag criada pelo robô dispara o workflow de novo**: use `tags-ignore: v*`.
- **Gradle usa `app/signing.properties` local + env vars no CI**: assinatura condicional (`releaseSigningConfigured`) — debug builda sem chave, release falha com mensagem clara.

---

## 4. Auto-update sem Play Store (self-update)

### Arquitetura
```
CI → APK + version.json no R2 → app lê version.json → compara build
→ diálogo → DownloadManager → instalador do Android
```
`version.json`: `{version, build, date, url, sha256}` com `Cache-Control: no-cache`.

### As 4 causas do "problema ao analisar o pacote" (ordem de investigação)
1. **Sem permissão "fontes desconhecidas"** (Android 8+): sem `canRequestPackageInstalls()`, o instalador nem abre e o erro é genérico. Cheque com `appops get <pkg> REQUEST_INSTALL_PACKAGES`. Fix: redireciona para `ACTION_MANAGE_UNKNOWN_APP_SOURCES` **antes** de baixar.
2. **FileProvider + scoped storage** (Android 10+): `File(/sdcard/Download/...)` é ilegível para o app; a URI sai válida mas com 0 bytes. Fix: `DownloadManager.getUriForDownloadedFile(id)` (`content://downloads/...`), lido direto pelo instalador.
3. **Receivers empilhados**: cada toque em "Atualizar" registra um receiver; todos disparam juntos. Fix: **single-flight** — cancela download/receiver pendente antes de enfileirar.
4. **Arquivo compartilhado**: downloads concorrentes sobrescrevem o mesmo `.apk`. Fix: nome único por build (`app-<versão>-<build>.apk`).

### Verificação de integridade (antes de culpar o código)
```powershell
sha256sum baixado.apk          # bate com version.json?
apksigner verify baixado.apk   # silencioso = válido
adb install -r baixado.apk     # Success = APK bom, bug é no Intent
```

### Limpeza
Ao instalar, remova downloads antigos (`dm.remove`) e apague `app-*.apk` órfãos — cliente nunca acumula versões.

### Permissões e manifest
- `REQUEST_INSTALL_PACKAGES` (normal, sem prompt) + checagem ativa via `canRequestPackageInstalls()`.
- `FileProvider` só se precisar compartilhar arquivo próprio; para self-update via DownloadManager, a URI do sistema basta.

---

## 5. UX do diálogo de update (padrão dos grandes apps)

- **Mostre só `versionName`** (`v0.2.0`). Build number é metadado interno — `‑0.2.0 (9)` confunde leigo (parece erro).
- **Persuada pelo benefício, não pelo número**: changelog ("O que mudou") converte mais que versão.
- **Antecipe o medo da permissão**: "pode permitir sem medo — é a própria [App] pedindo passagem". Dê o passo a passo dentro do modal.
- **Trate a marca no gênero certo** em todos os textos visíveis (varra com grep: `o App` vs `a App`).
- **Rodízio de mensagens** (título+corpo sorteados, sem repetir em sequência) evita fadiga em updates frequentes.
- **WhatsApp/Nubank/Play In-App Updates** nunca mostram build — siga o padrão quando sair de fase de testes.

---

## 6. Monorepo (app + backend no mesmo repo)

```
repo/
├── android/     # projeto Gradle (abrir aqui no Android Studio)
├── backend/     # API/servidor (contrato primeiro, código depois)
└── .github/     # CI na raiz (o Actions só lê de lá)
```
- Workflow com `paths: android/**` + `working-directory: android` — push no backend não dispara build de APK.
- Pasta em minúsculas, sem sufixo de linguagem (`android/`, não `APK_KOTLIN/`).
- `local.properties` e `signing.properties` ignorados em todos os níveis.
- Backend vazio precisa de `.gitkeep` (git ignora pasta vazia) + README com o contrato da API antes do código.

---

## 7. Checklist de novo app (copie e cole)

- [ ] Keystore gerado + backup (arquivo + senhas em gerenciador)
- [ ] `signing.properties` local, `.example` no git, ambos no `.gitignore`
- [ ] Secrets no GitHub: `KEYSTORE_BASE64` (linha única), `KEYSTORE_PASSWORD`, `KEY_ALIAS`
- [ ] Workflow: JDK 17, setup-android v4 sem `tools`, sdkmanager com platform/build-tools do `compileSdk`
- [ ] `versionCode` via run number (fallback local), `versionName` semântico
- [ ] Release com tag `vX.Y.Z` + APK anexado + `version.json` com `date`
- [ ] App: `REQUEST_INSTALL_PACKAGES`, checagem antes de baixar **e** antes de instalar, URI do DownloadManager, single-flight, limpeza de APKs velhos
- [ ] Diálogo: só versionName, benefício honesto, gênero certo, passo a passo da permissão
- [ ] Teste ponta a ponta: instala release → bump → push → diálogo → atualiza por cima sem desinstalar