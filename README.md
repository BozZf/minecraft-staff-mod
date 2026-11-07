# Staff Mod

Mod para Minecraft Java 1.20.2 com Forge 48.1.0. O cajado usa o modelo e as
texturas do projeto e dispara uma esfera roxa com a aparência da bola de fogo
do Ender Dragon.

## Como usar

Encontre o cajado na aba **Combate** do inventário criativo ou use
`/give @s staffmod:staff`. No modo sobrevivência, coloque dois lápis-lazúli
nos cantos superior direito e inferior esquerdo da bancada, com um graveto no
centro.

Aponte e clique com o botão direito. O cajado pode disparar uma vez por segundo.
A altura dos pés do jogador no momento do impacto determina a direção.

| Bloco atingido | Efeito |
| --- | --- |
| Acima do jogador | Cai como areia e volta a ser um bloco ao pousar. |
| Abaixo do jogador | Sobe como areia com a gravidade invertida e se encaixa sob o teto quando houver espaço. |
| Na mesma camada dos pés | Fica no lugar. |

O efeito precisa de espaço livre na direção do movimento. Blocos
inquebráveis, blocos com entidade de bloco (como baús), camas, blocos de duas
metades (como portas) e blocos sem colisão ficam no lugar. Ao tocar uma
entidade, o projétil desaparece sem causar dano ou criar uma nuvem de bafo.
Se o bloco subir até o limite de altura ou bater sem conseguir se encaixar,
ele vira um item quando a regra `doEntityDrops` estiver habilitada.
Depois de recolocado, o bloco volta a seguir suas próprias regras. Areia,
por exemplo, pode cair novamente se ficar sem apoio.

## Compilar e testar

Use um **JDK 17**. O Gradle Wrapper baixa as dependências do Minecraft e do
Forge na primeira execução.

| Tarefa | Linux e macOS | Windows |
| --- | --- | --- |
| Compilar o mod | `bash gradlew build` | `gradlew.bat build` |
| Rodar os testes no servidor Forge | `bash gradlew runGameTestServer` | `gradlew.bat runGameTestServer` |
| Abrir o cliente de desenvolvimento | `bash gradlew runClient` | `gradlew.bat runClient` |

Os GameTests disparam o cajado e verificam a queda, a subida com preservação
da orientação do bloco, a recarga e os impactos que devem deixar o bloco no
lugar. Para conferir o visual no cliente, dispare contra um bloco suspenso e
depois contra um bloco abaixo dos seus pés com um teto alguns blocos acima.

O JAR fica em `build/libs/`. Instale-o na pasta `mods` do perfil Forge
1.20.2. Em multiplayer, instale o mod no cliente e no servidor.

Caches, arquivos de compilação, logs e mundos de desenvolvimento ficam em
diretórios locais ignorados pelo Git, como `.gradle/`, `build/` e `run/`.
