/** Onde o passo acontece (não confundir com "ambiente" da GMUD). */
export type FlowLocation = 'HML' | 'PROD' | 'GITHUB';

export interface DeployFlowStep {
  order: number;
  title: string;
  location: FlowLocation;
  summary: string;
  detail: string;
}

export const DEPLOY_FLOW_STEPS: DeployFlowStep[] = [
  {
    order: 1,
    title: 'Registrar tarefa',
    location: 'HML',
    summary: 'Descreva o que vai ser feito antes de codar.',
    detail:
      'Em Homologação (dev.erpcorporativo.shop), menu Tarefas → Nova tarefa. O registro fica só no banco MySQL de HML — não existe em Produção.'
  },
  {
    order: 2,
    title: 'Iniciar tarefa',
    location: 'HML',
    summary: 'Marca IN_PROGRESS e mostra a branch sugerida.',
    detail:
      'No detalhe da tarefa, clique em Iniciar. Use a branch sugerida (ex.: feature/TASK-1-...) no Git.'
  },
  {
    order: 3,
    title: 'Desenvolver e enviar para dev',
    location: 'GITHUB',
    summary: 'Código na branch → merge ou push na branch dev.',
    detail:
      'Implemente localmente, abra PR ou faça push em dev. O GitHub é a fonte da verdade do código; HML e PROD só recebem imagens Docker depois.'
  },
  {
    order: 4,
    title: 'Deploy automático em HML',
    location: 'GITHUB',
    summary: 'Push em dev dispara o pipeline Deploy HML.',
    detail:
      'GitHub Actions builda imagens :hml, atualiza a VPS e registra uma GMUD de ambiente HML (aprovação automática). Você não precisa clicar em nada na GMUD de HML.'
  },
  {
    order: 5,
    title: 'Testar em Homologação',
    location: 'HML',
    summary: 'Validar a funcionalidade no site de testes.',
    detail:
      'Acesse https://dev.erpcorporativo.shop e confira se está tudo certo. Só avance para produção depois deste OK.'
  },
  {
    order: 6,
    title: 'Rascunho GMUD (opcional)',
    location: 'HML',
    summary: 'Planejamento com vínculo à tarefa — ainda no banco de HML.',
    detail:
      'Botão "Rascunho GMUD" na tarefa cria um registro environment=PROD no banco de HML, com taskId. Serve para documentar rollback e impacto antes do merge. Não é a GMUD que o pipeline de Produção aprova.'
  },
  {
    order: 7,
    title: 'Merge dev → main',
    location: 'GITHUB',
    summary: 'Código aprovado em HML vai para a branch main.',
    detail:
      'Abra PR dev → main no GitHub, revise e faça merge. Isso dispara o workflow Deploy PROD.'
  },
  {
    order: 8,
    title: 'GMUD real + deploy em Produção',
    location: 'PROD',
    summary: 'Pipeline cria GMUD no banco de PROD — você aprova lá.',
    detail:
      'Em https://erpcorporativo.shop → GMUD / Deploy: aparece uma mudança nova (criada pelo CI em api.erpcorporativo.shop). Aprove manualmente e aguarde o pipeline (até 15 min de polling). Não reexecute o workflow manualmente após aprovar.'
  },
  {
    order: 9,
    title: 'Testar em Produção',
    location: 'PROD',
    summary: 'Smoke test no site real.',
    detail:
      'Confira https://erpcorporativo.shop após o deploy. GMUD deve ir para status DEPLOYED.'
  },
  {
    order: 10,
    title: 'Concluir tarefa',
    location: 'HML',
    summary: 'Fecha o ciclo no mesmo lugar onde a tarefa nasceu.',
    detail:
      'Volte ao dev.erpcorporativo.shop → Tarefas → detalhe → Concluir. Status DONE fica registrado no banco de HML. A tarefa não precisa ser recriada em Produção.'
  }
];

export const FLOW_LOCATION_LABEL: Record<FlowLocation, string> = {
  HML: 'Homologação',
  PROD: 'Produção',
  GITHUB: 'GitHub / CI'
};

export const FLOW_LOCATION_BADGE: Record<FlowLocation, string> = {
  HML: 'bg-info text-dark',
  PROD: 'bg-danger',
  GITHUB: 'bg-dark'
};
