export const environment = {
  production: false,
  deployTier: 'HML' as const,
  hmlAppUrl: 'https://dev.erpcorporativo.shop',
  prodAppUrl: 'https://erpcorporativo.shop',
  /** Homologação: frontend em dev.erpcorporativo.shop, API em api.dev.erpcorporativo.shop */
  apiUrl: 'https://api.dev.erpcorporativo.shop',
  simularPagamentoPedido: false
};
