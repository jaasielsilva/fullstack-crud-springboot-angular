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
import { AccessDeniedComponent } from './pages/access-denied/access-denied.component';
import { PedidosComponent } from './pages/pedidos/pedidos.component';
import { CadastroTrialComponent } from './pages/cadastro-trial/cadastro-trial.component';
import { PlanosComponent } from './pages/planos/planos.component';
import { PagamentoSucessoComponent } from './pages/pagamento/pagamento-sucesso.component';
import { PagamentoFalhaComponent } from './pages/pagamento/pagamento-falha.component';
import { PagamentoPendenteComponent } from './pages/pagamento/pagamento-pendente.component';
import { TrialExpiradoComponent } from './pages/trial-expirado/trial-expirado.component';
import { AdminAssinantesComponent } from './pages/admin/assinantes/admin-assinantes.component';
import { GmudListComponent } from './pages/gmud/gmud-list/gmud-list.component';
import { GmudFormComponent } from './pages/gmud/gmud-form/gmud-form.component';
import { GmudDetailComponent } from './pages/gmud/gmud-detail/gmud-detail.component';
import { TaskListComponent } from './pages/tarefas/task-list/task-list.component';
import { TaskFormComponent } from './pages/tarefas/task-form/task-form.component';
import { TaskDetailComponent } from './pages/tarefas/task-detail/task-detail.component';
import { authGuard } from './security/auth.guard';
import { subscriptionGuard } from './security/subscription.guard';
import { adminGuard } from './security/admin.guard';

const ROLES_DASHBOARD = ['ADMIN', 'GERENTE', 'VENDEDOR', 'SUPORTE'];

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
  { path: 'sem-permissao', component: AccessDeniedComponent, canActivate: [authGuard, subscriptionGuard] },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard, subscriptionGuard],
    data: { roles: ROLES_DASHBOARD }
  },
  {
    path: 'clientes',
    component: ClientesComponent,
    canActivate: [authGuard, subscriptionGuard],
    data: { roles: ROLES_DASHBOARD }
  },
  {
    path: 'produtos',
    component: ProdutosComponent,
    canActivate: [authGuard, subscriptionGuard],
    data: { roles: ROLES_DASHBOARD }
  },
  {
    path: 'pedidos',
    component: PedidosComponent,
    canActivate: [authGuard, subscriptionGuard],
    data: { roles: ROLES_DASHBOARD }
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
    path: 'minha-assinatura',
    component: PlanosComponent,
    canActivate: [authGuard, subscriptionGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'admin/assinantes',
    component: AdminAssinantesComponent,
    canActivate: [authGuard, subscriptionGuard, adminGuard]
  },
  {
    path: 'gmud',
    component: GmudListComponent,
    canActivate: [authGuard, subscriptionGuard, adminGuard]
  },
  {
    path: 'gmud/nova',
    component: GmudFormComponent,
    canActivate: [authGuard, subscriptionGuard, adminGuard]
  },
  {
    path: 'gmud/:id',
    component: GmudDetailComponent,
    canActivate: [authGuard, subscriptionGuard, adminGuard]
  },
  {
    path: 'tarefas',
    component: TaskListComponent,
    canActivate: [authGuard, subscriptionGuard, adminGuard]
  },
  {
    path: 'tarefas/nova',
    component: TaskFormComponent,
    canActivate: [authGuard, subscriptionGuard, adminGuard]
  },
  {
    path: 'tarefas/:id',
    component: TaskDetailComponent,
    canActivate: [authGuard, subscriptionGuard, adminGuard]
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
