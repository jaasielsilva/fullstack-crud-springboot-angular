import { Routes } from '@angular/router';
import { ClientesComponent } from './pages/clientes/clientes.component';
import { ProdutosComponent } from './pages/produtos/produtos.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { LoginComponent } from './pages/login/login.component';
import { ConfiguracoesComponent } from './pages/configuracoes/configuracoes.component';
import { ForgotPasswordComponent } from './pages/forgot-password/forgot-password.component';
import { UsuariosComponent } from './pages/usuarios/usuarios.component';
import { EmpresasComponent } from './pages/empresas/empresas.component';
import { ResetPasswordForcedComponent } from './pages/reset-password-forced/reset-password-forced.component';
import { authGuard } from './security/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password-forced', component: ResetPasswordForcedComponent, canActivate: [authGuard] },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'clientes', component: ClientesComponent, canActivate: [authGuard] },
  { path: 'produtos', component: ProdutosComponent, canActivate: [authGuard] },
  { path: 'empresas', component: EmpresasComponent, canActivate: [authGuard], data: { roles: ['ADMIN'] } },
  { path: 'configuracoes', component: ConfiguracoesComponent, canActivate: [authGuard], data: { roles: ['ADMIN'] } },
  { path: 'usuarios', component: UsuariosComponent, canActivate: [authGuard], data: { roles: ['ADMIN'] } }
];
