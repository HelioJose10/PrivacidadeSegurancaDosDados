# Projeto Peer-to-Peer de Mensagens

Este projeto implementa uma aplicação de mensagens baseada em um sistema peer-to-peer (P2P) utilizando criptografia para garantir a segurança das mensagens enviadas. O sistema utiliza Java para a implementação do backend e Swing para a interface gráfica.


## Pré-requisitos

- Java Development Kit (JDK) 8 ou superior
- IDE de sua escolha (opcional, mas recomendado)
  
## Instalação

1. Clone este repositório:
   ```bash
   git clone https://seu-repositorio.git
   cd projeto

## Como Usar

Inicie a Aplicação: Execute o programa e uma janela gráfica será aberta.
Adicionar Destinatários: Você pode adicionar outros peers (destinatários) que estão rodando na mesma rede local.
Enviar Mensagens: Selecione um destinatário e digite sua mensagem para enviá-la.
Receber Mensagens: Mensagens enviadas por outros peers aparecerão na interface.

## Funcionamento

O sistema utiliza criptografia simétrica e assimétrica para garantir a segurança das mensagens:

Criptografia Assimétrica (RSA): Usada para trocar chaves simétricas entre os peers.
Criptografia Simétrica (AES): Usada para criptografar as mensagens em si.

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

