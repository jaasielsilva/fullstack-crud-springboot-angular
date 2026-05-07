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

      if (error.status === 401 && !isLoginRequest) {
        // 401 = nao autenticado / token invalido / expirado: forca logout
        console.warn('[Auth] Token expirado ou invalido. Encerrando sessao...');
        authService.logout();
      } else if (error.status === 403 && !isLoginRequest) {
        // 403 = autenticado mas sem permissao no recurso: nao desloga, apenas avisa
        console.warn('[Auth] Sem permissao para acessar', req.url);
        if (router.url !== '/sem-permissao') {
          router.navigate(['/sem-permissao']);
        }
      }

      return throwError(() => error);
    })
  );
};
