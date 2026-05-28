export type DeployTier = 'HML' | 'PROD';

export interface AppEnvironment {
  production: boolean;
  deployTier: DeployTier;
  hmlAppUrl: string;
  prodAppUrl: string;
  apiUrl: string;
  simularPagamentoPedido: boolean;
}
