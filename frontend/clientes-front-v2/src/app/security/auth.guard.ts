import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const authService = inject(AuthService);
  const token = authService.getToken();

  if (!token) {
    router.navigate(['/login']);
    return false;
  }

  const userInfo = authService.getUserInfo();
  // Se precisa redefinir senha e não está na página de redefinição, força o redirecionamento
  if (userInfo.mustChangePassword && state.url !== '/reset-password-forced') {
    router.navigate(['/reset-password-forced']);
    return false;
  }

  // Se a rota pedir roles específicos, verificamos
  const expectedRoles = route.data['roles'] as Array<string>;
  if (expectedRoles && expectedRoles.length > 0) {
    const hasRole = authService.hasRole(expectedRoles);
    if (!hasRole) {
      // Sem permissao: manda para a tela informativa, evitando loop com 403 do dashboard
      if (state.url !== '/sem-permissao') {
        router.navigate(['/sem-permissao']);
      }
      return false;
    }
  }

  return true;
};
