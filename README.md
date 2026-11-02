# Evoluir — Android

Aplicativo Android nativo (Kotlin/Jetpack Compose) de acompanhamento de pontos de melhoria.

## Funcionalidades
- Cadastro de pontos com nome, descrição e escolha entre 16 ícones lineares.
- Registro diário de falha ou superação, com notas opcionais.
- Calendário de histórico; dias sem registro permanecem neutros.
- Percentual calculado a partir dos dias **registrados** nos últimos 30 dias.
- Escala visual de cinco cores e gradiente (vermelho a verde).
- Widget de tela inicial com ícones coloridos e botões de registro.
- Compatibilidade com widgets na tela de bloqueio condicionada ao Android e ao fabricante.
- Dados locais via SQLite, sem login ou telemetria.

## APK de testes pelo GitHub Actions
Abra **Actions > Android debug APK > último workflow bem-sucedido > Artifacts > evoluir-debug-apk**.
O workflow também inicia automaticamente quando há push em `main`.

Para compilar localmente, use Android SDK 35, JDK 17 e Gradle 8.11.1:
```sh
gradle --no-daemon :app:assembleDebug
```
A instalação da versão 0.2 usa o mesmo applicationId da versão 0.1. Os dados existentes são preservados em atualizações normais, assinadas pela mesma chave.
