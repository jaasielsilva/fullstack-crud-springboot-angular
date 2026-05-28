export type ChangeType = 'NORMAL' | 'EMERGENCY' | 'STANDARD';
export type ChangeStatus = 'OPEN' | 'IN_APPROVAL' | 'APPROVED' | 'DEPLOYED' | 'ROLLBACK';
export type DeployEnvironment = 'DEV' | 'HML' | 'PROD';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export interface ChangeLog {
  id: number;
  fromStatus: ChangeStatus | null;
  toStatus: ChangeStatus;
  changedBy: string;
  changedAt: string;
  comment: string | null;
}

export interface ChangeRequest {
  id: number;
  title: string;
  description: string | null;
  type: ChangeType;
  status: ChangeStatus;
  environment: DeployEnvironment;
  riskLevel: RiskLevel;
  impactDescription: string | null;
  rollbackPlan: string | null;
  deploymentWindowStart: string | null;
  deploymentWindowEnd: string | null;
  createdAt: string;
  approvedAt: string | null;
  deployedAt: string | null;
  rolledBackAt: string | null;
  createdBy: string;
  version: string | null;
  artifact: string | null;
  pipelineRunId: string | null;
  commitSha: string | null;
  logs?: ChangeLog[];
}

export interface CreateChangeRequest {
  title: string;
  description?: string;
  type: ChangeType;
  environment: DeployEnvironment;
  riskLevel: RiskLevel;
  impactDescription?: string;
  rollbackPlan?: string;
  deploymentWindowStart?: string | null;
  deploymentWindowEnd?: string | null;
}
