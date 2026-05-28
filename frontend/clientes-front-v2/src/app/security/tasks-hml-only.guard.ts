import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { showTasksModuleInUi } from '../shared/deploy-flow/platform-ops.context';

/** Em Produção, redireciona /tarefas para GMUD (módulo de tarefas só em homologação). */
export const tasksHmlOnlyGuard: CanActivateFn = () => {
  if (showTasksModuleInUi()) {
    return true;
  }
  inject(Router).navigate(['/gmud']);
  return false;
};
