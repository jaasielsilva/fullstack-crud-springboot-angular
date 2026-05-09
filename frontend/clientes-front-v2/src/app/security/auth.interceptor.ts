import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  let requestToForward = req;

  if (token) {
    requestToForward = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(requestToForward).pipe(
    catchError((error: HttpErrorResponse) => {
      const isLoginRequest = req.url.includes('/auth/login');

      // 401 = não autenticado / token inválido: encerra sessão
      if (error.status === 401 && !isLoginRequest) {
        console.warn('[Auth] Token expirado ou inválido. Encerrando sessão...');
        authService.logout();
      } else if (error.status === 403 && !isLoginRequest) {
        const codigo = (error.error && (error.error as { codigo?: string }).codigo) || '';
        if (codigo === 'EMPRESA_BLOQUEADA') {
          router.navigate(['/planos']);
        } else {
          console.warn('[Auth] Sem permissão para acessar', req.url);
          if (router.url !== '/sem-permissao') {
            router.navigate(['/sem-permissao']);
          }
        }
      }

      return throwError(() => error);
    })
  );
};
