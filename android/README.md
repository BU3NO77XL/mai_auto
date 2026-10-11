# Mai — Automação Shopee

App Android de automação para afiliados da Shopee. Controla o celular via Serviço de Acessibilidade do Android para navegar de forma autônoma: abre o app, vai até a tela de criação de vídeo, entra na galeria, seleciona vídeo, preenche legenda, anexa produtos afiliados e publica — sem intervenção humana.

**Stack:** Android SDK 35 · Kotlin 2.0.21 · AGP 8.9.2 · JDK 17 · Gradle 8.11.1 · Views nativas (sem Compose)

---

## O que funciona hoje (v0.2.0)

### Autoteste autônomo completo
- Abre a Shopee automaticamente
- Navega: Home → aba Live/Vídeo → botão criar → aba Vídeo → Biblioteca
- Confirma que a galeria abriu (sem selecionar nenhum vídeo)
- Retorno limpo: swipe down para fechar galeria → X da câmera → Home Android → volta para o Mai
- Registra resultado com passo, mensagem e timestamp

### Motor de fluxo (FlowEngine)
- Busca de nós por resource-id, content-desc e text (nessa ordem de prioridade)
- Clique em 2 camadas: `ACTION_CLICK` subindo hierarquia (até 6 pais) → `dispatchGesture` fallback
- `waitStable()`: aguarda a árvore parar de mudar antes de agir (evita cliques em tela transitória)
- `swipeVertical()` com frações padrão de 0.72→0.30
- Captura de toasts da Shopee via `TYPE_NOTIFICATION_STATE_CHANGED`
- Diagnóstico de tela: dump de textos/descs + activity + último toast

### Seletores reais mapeados via ADB (Shopee BR atual)
| Chave | resource-id |
|---|---|
| Campo de legenda | `com.shopee.br.dfpluginshopee16:id/et_caption` |
| Adicionar produto | `com.shopee.br.dfpluginshopee16:id/add_product_panel_view` |
| Toggle rótulo IA | `com.shopee.br.dfpluginshopee16:id/ai_generated_toggle` |
| Permitir reutilização | `com.shopee.br.dfpluginshopee16:id/allow_reuse_toggle` |
| Botão Postar | `com.shopee.br.dfpluginshopee16:id/btn_post` |
| Botão Rascunhos | `com.shopee.br.dfpluginshopee16:id/tv_drafts_box` |
| Botão Biblioteca | `com.shopee.br:id/ll_gallery_entrance` |
| X câmera (voltar) | `com.shopee.br:id/iv_back` |
| X galeria (fechar) | `com.shopee.br:id/iv_close` |
| Próximo galeria | `com.shopee.br:id/tv_pick_next` |
| Próximo edição | `com.shopee.br:id/tv_compress` |
| Aba Live e Vídeo | content-desc: `tab_bar_button_video_and_live` |
| Botão criar | content-desc: `click top right create icon` |

> **Nota:** a tela de publicação usa o pacote `com.shopee.br.dfpluginshopee16` (plugin dinâmico). O `roots()` aceita qualquer `com.shopee.*`.

### ProductionFlowService
Serviço de foreground para execução do fluxo completo de produção (wakelock 10 min, coroutines). Recebe via Intent:
- `legenda` — texto da legenda
- `produtos` — lista de links de afiliado
- `modo` — `"publicar"` ou `"rascunho"`
- `rotulo_ia` — boolean para ligar/desligar rótulo de IA

### Interface (5 páginas)
- **Início** — dashboard: status de acessibilidade + Shopee, atalhos para teste e produção
- **Teste** — dispara autoteste, mostra último resultado com estado (sucesso/falha/inconclusivo)
- **Produção** — formulário: legenda, links de produtos, modo publicar/rascunho, toggle rótulo IA
- **Permissões** — checklist: Shopee, acessibilidade, vídeos, notificações, bateria, não perturbe, tela de bloqueio
- **Perfil** — apresentação visual estática; dados e itens são demonstrativos, sem ações implementadas

---

## Como abrir e executar

```powershell
# Build e instalar via ADB (celular conectado com depuração USB ativa)
cd android
.\gradlew assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Preparar o keystore para publicar o app

Pense no keystore como a **chave de identidade do app**: ele prova que uma atualização veio de quem publicou a versão anterior. Por isso, guarde o arquivo e a senha em segurança. Use sempre o mesmo arquivo `mai-upload.p12` nas próximas versões.

Siga estes passos no Windows. Os comandos devem ser executados no PowerShell, aberto na pasta deste projeto.

1. **Se ainda não tem um keystore, crie-o.** Esse comando cria a pasta e o arquivo que guardará a chave:

   ```powershell
   New-Item -ItemType Directory -Force "$env:USERPROFILE\.android\keystores"
   keytool -genkeypair -v -keystore "$env:USERPROFILE\.android\keystores\mai-upload.p12" -storetype PKCS12 -alias mai-upload -keyalg RSA -keysize 3072 -validity 10000
   ```

   O programa fará perguntas. Crie uma senha e guarde-a. Se ele pedir uma senha separada para a chave, use a mesma senha. O nome `mai-upload` é o alias, ou seja, o nome da chave dentro do arquivo.

2. **Diga ao projeto onde está o arquivo.** Faça uma cópia do modelo:

   ```powershell
   Copy-Item .\signing.properties.example .\app\signing.properties
   ```

   Abra `app/signing.properties` e troque o caminho de exemplo pelo caminho completo do seu arquivo `.p12`. Por exemplo, se ele estiver em `C:\Users\Ana\chaves\mai-upload.p12`, escreva:

   ```properties
   storeFile=C:\\Users\\Ana\\chaves\\mai-upload.p12
   ```

   Deixe `storeType=PKCS12`. Em `keyAlias`, escreva o alias usado ao criar a chave (neste exemplo, `mai-upload`). Esse arquivo é uma instrução local para o Gradle encontrar sua chave; não compartilhe nem envie ao GitHub.

3. **Informe a senha ao Gradle.** No mesmo PowerShell, cole o comando. Digite a senha quando for solicitada; ela não aparecerá enquanto você digita:

   ```powershell
   $securePassword = Read-Host "Digite a senha do keystore" -AsSecureString
   $password = [System.Net.NetworkCredential]::new("", $securePassword).Password
   $env:MAI_KEYSTORE_PASSWORD = $password
   $env:MAI_KEY_PASSWORD = $password
   ```

   Isso deixa a senha disponível apenas nessa janela do PowerShell. Fechar a janela apaga essas variáveis; na próxima publicação, repita este passo.

4. **Gere o arquivo pronto para publicar.** Ainda nessa janela, execute:

   ```powershell
   .\gradlew.bat :app:assembleRelease
   ```

   O Gradle usa o caminho e o alias do passo 2, mais a senha do passo 3, para assinar o app. O APK gerado para compartilhar por link ficará em `app\build\outputs\apk\release\app-release.apk`.

**Importante:** faça uma cópia segura do keystore e guarde a senha em um gerenciador de senhas. Nunca envie o arquivo `.p12`, a senha ou `app/signing.properties` ao GitHub. O arquivo `signing.properties.example` é apenas um modelo e pode ser compartilhado.

**Pré-requisitos:**
1. Android Studio com JDK 17, SDK 35, Gradle 8.11.1
2. Shopee Brasil instalada no aparelho
3. Depuração USB ativa no celular

**Primeiro uso:**
1. Instale o APK
2. Abra **Permissões** → ative o Serviço de Acessibilidade Mai
3. A tela **Perfil** ainda é apenas visual; suas funcionalidades serão definidas posteriormente
4. Abra **Teste** → toque **Iniciar autoteste** → aguarde ~20s → resultado aparece automaticamente

---

## Arquitetura

```
br.com.hydra.automacao/
├── MainActivity.kt                  # Todas as 5 páginas (Views nativas)
└── automation/
    ├── HydraAccessibilityService.kt # Serviço de acessibilidade + leitura de nós
    ├── FlowEngine.kt                # Motor de fluxo: seletores, cliques, navegação, retorno
    ├── FlowConfig.kt                # Configuração versionada + FlowConfigStore (JSON)
    ├── AutotestService.kt           # Autoteste autônomo (foreground service)
    ├── ProductionFlowService.kt     # Fluxo completo de produção (foreground service)
    └── TestResultStore.kt           # Persistência do último resultado
```

---

## Próximas etapas

- [ ] Implementar seleção de vídeo específico (por posição ou data)
- [ ] Integração com backend: buscar Job via API, reportar conclusão
- [ ] Sincronização de FlowConfig remota (endpoint da API)
- [ ] FCM para disparar jobs em background
- [ ] Navegação completa de fluxo de produção (legenda → produto → postar)

---
