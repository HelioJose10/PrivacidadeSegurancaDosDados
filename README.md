# Projeto Peer-to-Peer de Mensagens

Este projeto implementa uma aplicação de mensagens baseada em um sistema peer-to-peer (P2P), 
utilizando criptografia para garantir a segurança das mensagens enviadas. O sistema 
é desenvolvido em Java e utiliza a biblioteca Swing para a interface gráfica.

# Contexto do Projeto

Este trabalho é realizado no âmbito do mestrado em segurança informática, 
na cadeira de privacidade e segurança dos dados, da Faculdade de Ciências 
da Universidade de Lisboa.

# Tecnologias Utilizadas

-**Java**: Linguagem de programação principal utilizada.

-**Swing**: Biblioteca Java para construção da interface gráfica do usuário.

-**AES**: Criptografia simétrica utilizada para criptografar mensagens.

-**Diffie-Hellman**: Utilizado para a troca segura de chaves entre os peers, permitindo 
que duas partes estabeleçam uma chave secreta compartilhada sobre um canal inseguro..

## Pré-requisitos

- Java Development Kit (JDK) 8 ou superior
- IDE de sua escolha (opcional, mas recomendamos Visual Studio Code)
  
## Instalação

1. Clone este repositório: git clone https://seu-repositorio.git
2. Navegue até o diretório do projeto: cd projeto

## Como Usar

-**Iniciando a Aplicação**
Compile o projeto em sua IDE ou use o terminal para compilar os arquivos .java.
Execute a classe principal do programa. Uma janela gráfica será aberta.

-**Adicionando Destinatários**
Para que a comunicação funcione, certifique-se de que outros peers (destinatários) 
estejam rodando na mesma rede local. O sistema atualiza a lista de destinatários automaticamente.

-**Enviando Mensagens**
Selecione um destinatário a partir da lista.
Digite sua mensagem no campo de texto.
Clique no botão "Enviar" para enviar a mensagem.

-**Recebendo Mensagens**
Mensagens enviadas por outros peers aparecerão automaticamente na interface, assim que forem recebidas.

## Funcionamento


- **Comunicação segura**: As mensagens são criptografadas usando AES e RSA.
- **Troca de chaves**: Implementação do protocolo Diffie-Hellman para troca segura de chaves.
- **Interface gráfica**: Utilização do Swing para criar uma interface intuitiva.
- **Atualizações em tempo real**: A interface é atualizada automaticamente com novas mensagens e destinatários.


## Contribuição

Contribuições são bem-vindas! Sinta-se à vontade para abrir um issue ou enviar um pull request.

## Licença

Este projeto está licenciado sob a MIT License.

## Estrutra

projeto/

│

├── src/

│   └── main/

│       └── java/

│           ├── Peer.java               // Classe principal

│           ├── PeerHandler.java         // Classe para tratar a comunicação

│           ├── PeerGUI.java             // Classe para a interface gráfica

│           ├── PeerGUIListener.java      // Interface para notificação de mensagens

│           └── ...                      // Outras classes que você possa ter

│

└── bin/                                 // Diretório onde as classes compiladas serão armazenadas


#### 'Peer'
- **Descrição**: Classe principal que gerencia a lógica do peer, incluindo o armazenamento de mensagens, comunicação com outros peers e gerenciamento de chaves.
- **Responsabilidades**:
  - Enviar e receber mensagens.
  - Armazenar mensagens recebidas.
  - Gerenciar a lista de peers conectados.

#### 'PeerHandler'
- **Descrição**: Classe responsável por gerenciar a comunicação com um peer remoto, incluindo a leitura e descriptografia de mensagens.
- **Responsabilidades**:
  - Manter a conexão com um peer.
  - Descriptografar mensagens recebidas usando chaves simétricas e públicas.
  - Processar a chave pública recebida e gerar uma chave secreta compartilhada.

#### 'PeerGUI'
- **Descrição**: Classe que implementa a interface gráfica do usuário para interação com o sistema.
- **Responsabilidades**:
  - Exibir uma lista de destinatários disponíveis.
  - Permitir a digitação e envio de mensagens.
  - Atualizar a interface em tempo real com novas mensagens e conversas.

#### 'PeerGUIListener'
- **Descrição**: Interface que define um contrato para notificações de novas mensagens recebidas.
- **Responsabilidades**:
  - Notificar a implementação sobre novas mensagens.

### Fluxo de Dados

1. O usuário seleciona um destinatário e digita uma mensagem na interface gráfica (`PeerGUI`).
2. A mensagem é enviada através da classe `Peer`, que gerencia a comunicação.
3. O `PeerHandler` se encarrega de enviar a mensagem para o peer remoto.
4. O peer remoto recebe a mensagem, descriptografa-a e a armazena.
5. A interface é atualizada automaticamente para exibir novas mensagens.

