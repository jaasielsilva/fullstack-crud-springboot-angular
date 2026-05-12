# Manual: fluxo cliente → produto → pedido → pagamento

Este guia descreve o uso ponta a ponta do ERP multi-tenant: cadastro de **cliente**, **produtos**, criação de **pedido**, **pagamento** (Mercado Pago ou simulação em desenvolvimento) e **entrega**.

## Pré-requisitos

1. Acesse o sistema com um usuário da sua empresa (tenant), por exemplo **ADMIN**, **GERENTE** ou **VENDEDOR**.
2. Configure o **Mercado Pago** no servidor (token e URL de notificação), conforme `docs/19-trial-assinatura-mercadopago.md`, para que o webhook confirme pagamentos de pedidos.
3. O frontend deve apontar para a API correta (`environment.apiUrl`).
4. **CORS:** a API aceita, por padrão, `http://localhost:4200` e `http://127.0.0.1:4200` (Angular `ng serve`) além dos domínios de produção/homologação. Para outra origem, use `APP_CORS_ALLOWED_ORIGINS` (lista CSV) na API.

---

## 1. Cadastrar um cliente

1. No menu, abra **Clientes** (ou equivalente na sua navegação).
2. Cadastre um cliente com **nome** e, se possível, **e-mail** (o e-mail é usado como pagador na preferência Mercado Pago; se estiver vazio, um placeholder é enviado).
3. Mantenha o cliente **ativo** para aparecer na lista ao montar pedidos.

Somente clientes do **mesmo tenant** da sessão são listados e podem ser vinculados a pedidos.

---

## 2. Cadastrar produtos

1. Abra **Produtos**.
2. Cadastre produtos com **preço**, **quantidade em estoque** e status **ativo**.
3. O estoque é **baixado** na criação/edição do pedido e **devolvido** se o pedido em **ABERTO** for cancelado, excluído ou reeditado (itens antigos são devolvidos antes de aplicar os novos).

---

## 3. Criar um pedido

1. Abra **Pedidos** e clique em **Inserir**.
2. Selecione o **cliente** e adicione **linhas de produto** com quantidades válidas (não pode ultrapassar o estoque disponível).
3. Confirme com **Cadastrar Pedido**.

O pedido nasce com status **ABERTO**. Só pedidos **ABERTO** podem ser **editados** ou **excluídos** pela interface atual.

---

## 4. Pagar o pedido (Mercado Pago)

1. Na listagem de pedidos, com status **ABERTO**, clique no botão verde de **cartão** (Pagar).
2. A API cria uma **preferência** no Mercado Pago e o navegador é redirecionado para o checkout.
3. Após pagar, o Mercado Pago redireciona de volta para `/pedidos` (parâmetros `mp=success`, `mp=failure` ou `mp=pending`).
4. A confirmação oficial do pagamento vem pelo **webhook** da API: quando o status do pagamento na API do MP for **approved**, o pedido passa a **PAGO** e recebe **data de pagamento**.

**Empresa comercialmente bloqueada:** ainda é permitido o `POST /api/pedidos/{id}/checkout` para facilitar a regularização via pagamento de pedido (filtro `TenantAccessFilter`).

**Referência externa:** o pedido usa o formato `PEDIDO_{tenantId}_{pedidoId}` na cobrança, para o webhook associar o pagamento ao pedido correto.

---

## 5. Simular pagamento (apenas desenvolvimento / homologação)

Para testar sem o fluxo real do Mercado Pago:

1. **Backend:** defina `APP_PEDIDO_SIMULAR_PAGAMENTO=true` (ou `app.pedido-simular-pagamento=true` no perfil desejado). Em produção, mantenha **false**.
2. **Frontend:** em `environment.ts` de desenvolvimento, `simularPagamentoPedido: true` (em produção use `false`).
3. Faça login como **ADMIN** ou **GERENTE**.
4. Na lista, no pedido **ABERTO**, use o botão **raio** (Simular pagamento) e confirme.

O pedido passa a **PAGO** com data de pagamento preenchida, sem registrar transação no Mercado Pago.

---

## 6. Após o pagamento: entrega

1. Com o pedido em **PAGO**, use o botão de **caminhão** (Marcar entregue).
2. O status vai para **ENTREGUE**.

---

## 7. Cancelar pedido em aberto

1. Em pedido **ABERTO**, use o botão **cancelar** (X em destaque amarelo).
2. O estoque dos itens é **devolvido** e o status vai para **CANCELADO**.

Pedidos **PAGO** ou **ENTREGUE** não podem ser cancelados por este fluxo (exige tratamento financeiro/estorno).

---

## Resumo dos status

| Status      | Significado                          |
|------------|---------------------------------------|
| `ABERTO`   | Pedido criado; aguarda pagamento ou cancelamento |
| `PAGO`     | Pagamento confirmado (webhook ou simulação) |
| `ENTREGUE` | Mercadoria/serviço concluído após pagamento |
| `CANCELADO`| Pedido cancelado em aberto; estoque devolvido |

---

## Endpoints da API (referência)

- `POST /api/pedidos` — criar  
- `PUT /api/pedidos/{id}` — alterar (somente **ABERTO**)  
- `GET /api/pedidos` — listar do tenant  
- `GET /api/pedidos/{id}` — detalhe  
- `DELETE /api/pedidos/{id}` — excluir (somente **ABERTO**)  
- `POST /api/pedidos/{id}/checkout` — URL de checkout Mercado Pago  
- `POST /api/pedidos/{id}/marcar-entregue` — **PAGO** → **ENTREGUE**  
- `POST /api/pedidos/{id}/cancelar` — **ABERTO** → **CANCELADO** (devolve estoque)  
- `POST /api/pedidos/{id}/simular-pagamento` — **ADMIN/GERENTE** + flag no servidor  

Todos exigem autenticação JWT e respeitam o **tenant** do usuário.
