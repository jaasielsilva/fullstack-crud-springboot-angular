export interface AssinanteAdmin {
  empresaId: number;
  nomeEmpresa: string;
  documento: string;
  email: string;
  statusEmpresa: string;
  planoNome: string;
  statusAssinatura: string;
  dataInicio: string;
  dataVencimento: string;
  ultimoPagamentoStatus: string;
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
