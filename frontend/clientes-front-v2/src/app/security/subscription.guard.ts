import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Atualiza o contexto de assinatura via {@code GET /api/auth/me} e bloqueia navegação interna
 * quando a empresa está {@code BLOQUEADA} ou {@code CANCELADA}.
 */
export const subscriptionGuard: CanActivateFn = (_route, state) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  if (!authService.getToken()) {
    return true;
  }

  return authService.refreshSubscriptionContext().pipe(
    map(() => {
      const snap = authService.getSubscriptionContext();
      if (!snap) {
        return true;
      }

      const blocked = snap.empresaStatus === 'BLOQUEADA' || snap.empresaStatus === 'CANCELADA';
      if (!blocked) {
        return true;
      }

      const url = state.url.split('?')[0];
      const allowedPrefixes = [
        '/planos',
        '/pagamento/sucesso',
        '/pagamento/falha',
        '/pagamento/pendente',
        '/trial-expirado',
        '/reset-password-forced',
        '/sem-permissao'
      ];
      if (allowedPrefixes.some((p) => url.startsWith(p))) {
        return true;
      }

      return router.parseUrl('/planos');
    }),
    catchError(() => of(true))
  );
};
