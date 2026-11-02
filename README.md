# Evoluir — Android

Aplicativo nativo para acompanhar comportamentos que você quer melhorar, registrando **superações** (verde) e **falhas** (vermelho) por dia. Sem conta, sem anúncios, sem Internet e sem telemetria.

## Versão 0.1.0

- Adicionar, editar e excluir pontos de melhoria (nome, descrição e ícone personalizável).
- Marcar **Superei** ou **Falhei** uma vez por data, podendo corrigir registros antigos.
- Nota opcional para cada dia; registros não preenchidos permanecem cinza, nunca são computados como falha.
- Histórico de 35 dias à vista, com seletor de qualquer data passada.
- Indicador contínuo de vermelho → laranja → amarelo → verde calculado pelos **últimos 30 dias corridos**, considerando somente dias registrados.
- Widget Android com até quatro pontos de melhoria, indicadores coloridos e botões interativos ✓ e ✕.
- Banco SQLite local: `evoluir.db`. Remover o aplicativo apaga os registros; exportação/backup ainda não implementados.

## Construção

**Observação:** o APK Android completo ainda precisa ser compilado em um ambiente com Android SDK; os testes de lógica do projeto passam, mas a compilação completa não foi executada neste ambiente.


Abra a pasta no **Android Studio** com Android SDK 35 e JDK 17, sincronize e execute a configuração `app`.

Também é possível construir pelo terminal, com Android SDK 35 e Gradle 8.11.1 instalados:

```bash
gradle :app:assembleDebug
```

O APK será produzido em `app/build/outputs/apk/debug/app-debug.apk`.

Um fluxo de GitHub Actions (`.github/workflows/android.yml`) gera automaticamente um APK de teste depois de enviar o projeto para `main`; baixe-o em **Actions → Android debug APK → Artifacts**.

## Widgets de bloqueio

Este projeto declara a categoria `home_screen|keyguard` para que o mesmo widget seja disponibilizado na tela de bloqueio **em versões e fabricantes que aceitem widgets de terceiros nesse local**. Isso não pode ser forçado pelo aplicativo; em muitos aparelhos somente a tela inicial será compatível. A interação pode exigir desbloqueio.

## Estrutura

- `MainActivity.kt`: interface Jetpack Compose.
- `ImprovementStore.kt`: armazenamento SQLite local.
- `Progress.kt`: cálculo de pontuação e interpolação das cores.
- `ProgressWidgetProvider.kt`: widget Android interativo usando RemoteViews.

## Privacidade e desenvolvimento

- Nenhuma permissão de rede.
- Nenhum dado é enviado ao GitHub: o GitHub hospeda somente o código-fonte.
- Não há sincronização/backup entre aparelhos nesta versão.
- O APK de teste do Actions será assinado com chave de debug, apenas para instalação e testes.
