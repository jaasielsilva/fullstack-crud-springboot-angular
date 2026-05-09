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
import { CadastroTrialComponent } from './pages/cadastro-trial/cadastro-trial.component';
import { PlanosComponent } from './pages/planos/planos.component';
import { PagamentoSucessoComponent } from './pages/pagamento/pagamento-sucesso.component';
import { PagamentoFalhaComponent } from './pages/pagamento/pagamento-falha.component';
import { PagamentoPendenteComponent } from './pages/pagamento/pagamento-pendente.component';
import { TrialExpiradoComponent } from './pages/trial-expirado/trial-expirado.component';
import { authGuard } from './security/auth.guard';
import { subscriptionGuard } from './security/subscription.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'cadastro-trial', component: CadastroTrialComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'checkout', redirectTo: 'planos', pathMatch: 'full' },
  {
    path: 'reset-password-forced',
    component: ResetPasswordForcedComponent,
    canActivate: [authGuard, subscriptionGuard]
  },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard, subscriptionGuard]
  },
  {
    path: 'clientes',
    component: ClientesComponent,
    canActivate: [authGuard, subscriptionGuard]
  },
  {
    path: 'produtos',
    component: ProdutosComponent,
    canActivate: [authGuard, subscriptionGuard]
  },
  {
    path: 'empresas',
    component: EmpresasComponent,
    canActivate: [authGuard, subscriptionGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'configuracoes',
    component: ConfiguracoesComponent,
    canActivate: [authGuard, subscriptionGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'usuarios',
    component: UsuariosComponent,
    canActivate: [authGuard, subscriptionGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'planos',
    component: PlanosComponent,
    canActivate: [authGuard, subscriptionGuard]
  },
  {
    path: 'pagamento/sucesso',
    component: PagamentoSucessoComponent,
    canActivate: [authGuard, subscriptionGuard]
  },
  {
    path: 'pagamento/falha',
    component: PagamentoFalhaComponent,
    canActivate: [authGuard, subscriptionGuard]
  },
  {
    path: 'pagamento/pendente',
    component: PagamentoPendenteComponent,
    canActivate: [authGuard, subscriptionGuard]
  },
  {
    path: 'trial-expirado',
    component: TrialExpiradoComponent,
    canActivate: [authGuard, subscriptionGuard]
  }
];
