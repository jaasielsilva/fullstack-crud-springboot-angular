import { AppEnvironment } from './environment.types';

export const environment: AppEnvironment = {
  production: false,
  deployTier: 'HML',
  hmlAppUrl: 'https://dev.erpcorporativo.shop',
  prodAppUrl: 'https://erpcorporativo.shop',
  /** Homologação: frontend em dev.erpcorporativo.shop, API em api.dev.erpcorporativo.shop */
  apiUrl: 'https://api.dev.erpcorporativo.shop',
  simularPagamentoPedido: false
};
