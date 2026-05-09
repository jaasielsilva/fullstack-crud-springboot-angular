export interface SubscriptionSnapshot {
  empresaStatus: string;
  assinaturaStatus: string;
  trialInicio?: string | null;
  trialFim?: string | null;
  diasTrialRestantes?: number | null;
  pagamentoPendente: boolean;
  mensagemPagamentoPendente?: string | null;
  recursosLiberados: string[];
}
