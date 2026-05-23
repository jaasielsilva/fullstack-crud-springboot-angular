export interface SubscriptionSnapshot {
  empresaStatus: string;
  assinaturaStatus: string;
  trialInicio?: string | null;
  trialFim?: string | null;
  diasTrialRestantes?: number | null;
  pagamentoPendente: boolean;
  mensagemPagamentoPendente?: string | null;
  /** Dias até o fim do período pago da assinatura ativa (alerta nos últimos 7 dias dos 30 pós-pagamento). */
  diasAteVencimentoAssinatura?: number | null;
  mensagemRenovacaoAssinatura?: string | null;
  recursosLiberados: string[];
}
