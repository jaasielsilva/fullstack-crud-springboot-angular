import { currentDeployTier } from './deploy-flow.context';

/** Tarefas e sino de notificações: apenas HML/local (planejamento). PROD usa só GMUD. */
export function showTasksModuleInUi(): boolean {
  return currentDeployTier() !== 'PROD';
}
