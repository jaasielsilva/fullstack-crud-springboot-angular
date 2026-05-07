import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  let requestToForward = req;

  if (token) {
    // Clona a requisição e adiciona o header Authorization
    requestToForward = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  // Retorna a requisição original ou a clonada com o Token, e fica "escutando" a resposta
  return next(requestToForward).pipe(
    catchError((error: HttpErrorResponse) => {
      // Se o Backend devolver Erro de Segurança (401 Não Autorizado ou 403 Proibido)
      if (error.status === 401 || error.status === 403) {
        // Importante: Só desloga se a requisição NÃO for para a própria rota de Login, 
        // senão quem erra a senha no login seria redirecionado num loop infinito!
        if (!req.url.includes('/auth/login')) {
          console.warn('⚠️ Token expirado ou adulterado! Forçando logout por segurança...');
          authService.logout();
        }
      }
      // Repassa o erro pra frente caso algum componente queira ler a mensagem
      return throwError(() => error);
    })
  );
};
