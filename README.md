# AFTERIMAGE — ARG de terror psicológico

Mod para Minecraft Java **1.21.11 + Fabric + Java 21**. A proposta é criar a sensação de que algo está errado no jogo: coincidências discretas no começo, estranheza crescente e um desfecho dentro do próprio Minecraft.

## Linha do tempo — 20 minutos de sobrevivência ativa
- **00:40** — mensagem ambiente discreta.
- **02:00** — sugestão de passos fora de ritmo.
- **04:00** — primeira mensagem inquietante.
- **06:00** — som curto e deslocado.
- **08:00** — mensagem de chat que parece não pertencer ali.
- **10:00** — ambiente de caverna fora de contexto.
- **12:00** — seis armor stands quase invisíveis formam uma massa de falhas a 15 blocos atrás do jogador.
- **14:00** — mensagens sugerem que o mundo percebe seus movimentos.
- **16:00** — referência à gravação.
- **18:00** — quebra da quarta parede dentro do jogo.
- **20:00** — cegueira, lentidão extrema e mensagem final vermelha. O jogo não fecha.

## Segurança
- Não usa `System.exit()`, não abre programas externos e não corrompe a tela de título.
- O progresso é salvo em `.opt_state_cache` dentro da pasta do mundo a cada 5 segundos de ticks.
- O cache é local ao mundo. Para reiniciar o evento, feche o Minecraft e remova esse arquivo da pasta do mundo.
- O cronômetro usa ticks do servidor; pausas e inatividade não contam como tempo ativo.

## Compilar
É necessário Java 21. Execute `gradle build` ou use o workflow **Build Afterimage ARG** na aba Actions do GitHub. O JAR compilado fica em `build/libs/`.
