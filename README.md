#  Projeto Prático: Escalabilidade de Aplicações Web com Load Balancer

Trabalho prático desenvolvido para a disciplina/atividade de infraestrutura e escalabilidade, com o objetivo de implementar, executar e comprovar o funcionamento de um balanceador de carga distribuindo requisições entre múltiplas instâncias de uma aplicação web desenvolvida em Java (Spring Boot).

---

##  Tecnologias Utilizadas

* **Java 21 / Spring Boot**: Construção da aplicação web mínima e rotas de suporte (`/` e `/health`).
* **NGINX**: Atua como o proxy reverso e balanceador de carga na porta principal (`8080`).
* **Docker & Docker Compose**: Orquestração e gerenciamento isolado de múltiplos containers (instâncias Java + NGINX).
* **Maven Wrapper (`mvnw`)**: Empacotamento do projeto Java sem dependência de instalação global do Maven.

---

##  Arquitetura da Solução

A arquitetura segue o modelo padrão de **escalabilidade horizontal com proxy reverso**:

1. O **Cliente** (navegador ou terminal) faz uma requisição exclusivamente para o **Load Balancer (NGINX)** na porta `8080`.
2. O **NGINX** intercepta a chamada e aplica a política de distribuição **Round-Robin**, encaminhando a requisição de forma alternada e cíclica para um dos containers de backend (`app-01`, `app-02`, `app-03` ou `app-04`).
3. Cada instância do Spring Boot lê uma variável de ambiente (`INSTANCE_ID`) injetada pelo Docker Compose, identificando-se visualmente na página HTML (com cores de fundo distintas) e através do cabeçalho HTTP personalizado `X-Served-By`.

```text
       [ Cliente / Navegador ]
                 │
                 ▼  (http://localhost:8080)
      ┌──────────────────────┐
      │     Load Balancer    │
      │       (NGINX)        │
      └──────────┬───────────┘
                 │
       ┌─────────┼─────────┬─────────┐
       ▼         ▼         ▼         ▼
    [App-01]  [App-02]  [App-03]  [App-04]
    (#dcecff) (#d9f4e3) (#ffe8cf) (#f0ddff)
```

---

##  Como Configurar o Balanceamento

O balanceamento foi configurado no arquivo `nginx/nginx.conf` utilizando um bloco `upstream` com a política padrão **Round-Robin**:

```nginx
events {}

http {
    upstream servidores_java {
        server app-01:8080;
        server app-02:8080;
        server app-03:8080;
        server app-04:8080;
    }

    server {
        listen 8080;

        location / {
            proxy_pass http://servidores_java;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }
}
```

---

##  Instruções de Execução (Passo a Passo)

Certifique-se de ter o Docker e o Java/Maven instalados na sua máquina.

1. Clonar ou abrir a pasta do projeto no terminal.
2. Gerar o arquivo `.jar` executável utilizando o Maven Wrapper:   
   * No Windows (PowerShell):
     ```powershell
     .\mvnw.cmd clean package
     ```
   * No Linux / macOS:
     ```bash
     ./mvnw clean package
     ```
3. Subir todo o ambiente (as instâncias da aplicação e o NGINX) com um único comando do Docker Compose:   
   ```bash
   docker compose up --build
   ```

---

##  Como Reproduzir os Testes de Verificação

Execute os testes descritos na seção de verificação da atividade:

1. **Teste Visual de Alternância:**
   * Acesse no navegador: `http://localhost:8080`.
   * Pressione `Ctrl + F5` sucessivas vezes. A cor de fundo e o nome da instância (`app-01`, `app-02`, etc.) mudarão sequencialmente a cada clique.

2. **Teste por Linha de Comando (Cabeçalho HTTP):**
   * No Windows (PowerShell), utilize o executável do cURL para inspecionar o cabeçalho de origem:
     ```powershell
     curl.exe -I http://localhost:8080
     ```
   * Verifique o retorno do cabeçalho personalizado `X-Served-By: app-XX`.

3. **Teste de Distribuição em Volume (100 Requisições):**
   * Para comprovar o balanceamento equilibrado de carga, execute o script abaixo no PowerShell:
     ```powershell
     $results = 1..100 | ForEach-Object { 
         (Invoke-WebRequest -Uri "http://localhost:8080" -UseBasicParsing -Method Head).Headers."X-Served-By" 
     }
     $results | Group-Object | Select-Object Name, Count
     ```

4. **Teste de Tolerância a Falhas (Resiliência):**
   * Com o ambiente rodando, derrube uma das instâncias via terminal:
     ```bash
     docker stop loadbalancer-app-01-1
     ```
   * Atualize a página no navegador ou rode novamente o teste de volume. O serviço continuará operacional, alternando perfeitamente apenas entre as instâncias restantes, sem erros para o usuário.
   * Reative a instância com o comando abaixo para vê-la retornar à rotação:
     ```bash
     docker start loadbalancer-app-01-1
     ```

5. **Prova Negativa:**
   * Tente acessar diretamente uma porta interna de backend e comprove que o indicador fixo não alterna, provando que o balanceamento é responsabilidade exclusiva do NGINX.

---

## 🔮 Evolução da Solução

Como a arquitetura foi desacoplada com containers e proxy reverso, a solução pode evoluir facilmente das seguintes formas:
* **Adição de Novas Instâncias:** Basta declarar novos serviços no arquivo de compose e incluí-los no bloco `upstream` do NGINX.
* **Sticky Sessions (Sessões Afinitas):** Caso a aplicação precise manter estado em memória, o NGINX pode ser configurado com diretivas como `ip_hash` ou `sticky cookie` para garantir que o mesmo usuário seja sempre direcionado para a mesma instância.
* **Auto Scaling (Escalabilidade Automática):** Em ambientes de produção em nuvem (como Kubernetes ou AWS ECS), o NGINX pode ser substituído por Load Balancers gerenciados (ALB) integrados a políticas de redimensionamento automático baseadas em consumo de CPU ou tráfego de rede.
