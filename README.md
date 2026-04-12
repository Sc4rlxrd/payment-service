# Serviço de Pagamento

[![Java](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.13-brightgreen)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)](https://www.postgresql.org/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.12-pink)](https://www.rabbitmq.com/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-blue)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

Um microsserviço responsável pelo processamento de pagamentos no sistema de e-commerce BookCommerce. Este serviço escuta eventos de criação de pedidos, simula o processamento de pagamentos e publica os resultados de volta na fila de mensagens.

## Funcionalidades

- Processamento assíncrono de pagamentos via RabbitMQ
- Simulação de pagamento com taxas de sucesso/falha configuráveis
- Persistência de registros de pagamento no banco de dados
- API REST para solicitações diretas de pagamento (atualmente comentada)
- Tratamento de exceções e gerenciamento global de erros

## Tecnologias Utilizadas

- **Java 21**
- **Spring Boot 3.5.13**
- **Spring Data JPA** para operações de banco de dados
- **Spring AMQP** para integração com RabbitMQ
- **PostgreSQL** como banco de dados
- **Flyway** para migrações de banco de dados
- **Lombok** para redução de código boilerplate
- **Docker** para configuração de banco de dados containerizado

## Pré-requisitos

- Java 21 ou superior
- Maven 3.6+
- PostgreSQL (ou Docker para configuração containerizada)
- Servidor RabbitMQ em execução

## Instalação

1. Clone o repositório:
   ```bash
   git clone <url-do-repositorio>
   cd payment-service
   ```

2. Configure o banco de dados:
   - Usando Docker Compose:
     ```bash
     docker-compose up -d
     ```
     Nota: O docker-compose.yml configura PostgreSQL na porta 5432, mas o application.yaml está configurado para a porta 5435. Atualize a porta no application.yaml se necessário.

   - Ou configure manualmente PostgreSQL na porta 5435 com:
     - Banco de dados: `payment_db`
     - Usuário: `postgres`
     - Senha: `root`

3. Certifique-se de que RabbitMQ está executando em localhost:5672 com as credenciais:
   - Usuário: `book_user`
   - Senha: `book_password`

## Configuração

A configuração da aplicação está em `src/main/resources/application.yaml`:

- Porta do servidor: 8083
- Banco de dados: PostgreSQL em localhost:5435
- RabbitMQ: localhost:5672
- JPA: DDL auto validate, show SQL habilitado
- Flyway: Habilitado para migrações

## Executando a Aplicação

1. Construa o projeto:
   ```bash
   mvn clean install
   ```

2. Execute a aplicação:
   ```bash
   mvn spring-boot:run
   ```

O serviço iniciará na porta 8083 e começará a escutar mensagens na fila `payment.process.queue`.

## Endpoints da API

Atualmente, a API REST está comentada. Quando habilitada, ela forneceria:

- `POST /payments` - Processar um pagamento diretamente (requer PaymentRequestDTO)

## Fluxo de Mensagens

1. **Evento de Pedido Criado**: Recebe `OrderCreatedEvent` do serviço de pedidos contendo orderId, amount e customerEmail.

2. **Processamento de Pagamento**: Simula o processamento de pagamento com:
   - 70% de chance de SUCESSO
   - 20% de chance de FALHA
   - 10% de chance de lançar PaymentException

3. **Publicação de Resultado**: Publica `PaymentResultEvent` para:
   - Fila de sucesso (para pagamentos bem-sucedidos)
   - Fila de falha (para pagamentos falhados)

## Esquema do Banco de Dados

A tabela `payments` armazena registros de pagamento:

```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

## Testes

Execute os testes com:
```bash
mvn test
```


## Licença

Este projeto está licenciado sob a Licença MIT.
