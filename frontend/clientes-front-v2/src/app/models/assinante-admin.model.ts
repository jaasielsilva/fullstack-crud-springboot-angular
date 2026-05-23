export interface AssinanteAdmin {
  empresaId: number;
  nomeEmpresa: string;
  documento: string;
  email: string;
  /** API pode enviar null em registros antigos ou sem assinatura. */
  statusEmpresa: string | null;
  planoNome: string;
  statusAssinatura: string | null;
  dataInicio: string;
  dataVencimento: string;
  ultimoPagamentoStatus: string | null;
  valorUltimoPagamento: number;
  ultimoPagamentoData: string;
  /** Dias até dataVencimento da última assinatura ATIVA; null se não aplicável. */
  diasAteVencimentoPlano?: number | null;
}

export interface AdminDashboardMetrics {
  totalAssinantesAtivos: number;
  mrrEstimado: number;
  empresasEmTrial: number;
  pagamentosPendentes: number;
  /** Última assinatura ATIVA com vencimento entre hoje e +7 dias. */
  renovacaoUrgente7Dias: number;
}
