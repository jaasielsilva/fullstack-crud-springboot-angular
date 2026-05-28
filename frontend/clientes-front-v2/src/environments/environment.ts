import { AppEnvironment } from './environment.types';

export const environment: AppEnvironment = {
  production: false,
  deployTier: 'HML',
  hmlAppUrl: 'https://dev.erpcorporativo.shop',
  prodAppUrl: 'https://erpcorporativo.shop',
  apiUrl: 'http://localhost:8080',
  /** Exibe ação "Simular pagamento" (ADMIN/GERENTE); o backend também exige `APP_PEDIDO_SIMULAR_PAGAMENTO=true`. */
  simularPagamentoPedido: true
};
