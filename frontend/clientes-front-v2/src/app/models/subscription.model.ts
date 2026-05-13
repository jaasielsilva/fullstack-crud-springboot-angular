export interface SubscriptionSnapshot {
  empresaStatus: string;
  assinaturaStatus: string;
  trialInicio?: string | null;
  trialFim?: string | null;
  diasTrialRestantes?: number | null;
  pagamentoPendente: boolean;
  mensagemPagamentoPendente?: string | null;
  /** Dias até o fim da assinatura ativa (alerta de renovação), quando enviado pelo backend. */
  diasAteVencimentoAssinatura?: number | null;
  mensagemRenovacaoAssinatura?: string | null;
  recursosLiberados: string[];
}
