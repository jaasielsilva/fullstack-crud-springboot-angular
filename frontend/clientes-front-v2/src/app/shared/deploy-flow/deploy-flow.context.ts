import { environment } from '../../../environments/environment';
import { ChangeRequest } from '../../models/gmud/change-request.model';
import { TaskStatus, WorkTask } from '../../models/task/work-task.model';

export type DeployTier = 'HML' | 'PROD';

export function currentDeployTier(): DeployTier {
  if (environment.deployTier === 'PROD' || environment.deployTier === 'HML') {
    return environment.deployTier;
  }
  return environment.production ? 'PROD' : 'HML';
}

/** Índice 0-based do passo "atual" sugerido para uma tarefa (1..10 → retorna 0..9). */
export function suggestCurrentStepIndex(task: WorkTask, gmuds: ChangeRequest[]): number {
  if (task.status === 'DONE' || task.status === 'CANCELLED') {
    return 9;
  }
  if (task.status === 'OPEN') {
    return 0;
  }

  const prodGmud = gmuds.find((g) => g.environment === 'PROD');
  const prodDeployed = prodGmud?.status === 'DEPLOYED';
  const prodApproved = prodGmud?.status === 'APPROVED' || prodDeployed;
  const hasDraftProd = !!prodGmud;
  const hmlDeployed = gmuds.some((g) => g.environment === 'HML' && g.status === 'DEPLOYED');

  if (prodDeployed) {
    return 8;
  }
  if (prodApproved) {
    return 7;
  }
  if (hasDraftProd) {
    return 6;
  }
  if (hmlDeployed) {
    return 4;
  }
  if (task.status === 'IN_PROGRESS') {
    return 2;
  }
  return 1;
}
