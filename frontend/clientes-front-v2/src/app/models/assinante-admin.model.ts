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
}

export interface AdminDashboardMetrics {
  totalAssinantesAtivos: number;
  mrrEstimado: number;
  empresasEmTrial: number;
  pagamentosPendentes: number;
}
