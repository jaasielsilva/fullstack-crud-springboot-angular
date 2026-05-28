export type TaskStatus = 'OPEN' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED';

export interface WorkTask {
  id: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  branchName: string | null;
  createdAt: string;
  updatedAt: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdBy: string;
  linkedChangeId: number | null;
}

export interface CreateWorkTask {
  title: string;
  description?: string;
}

export interface PendingTasksResponse {
  pendingCount: number;
  tasks: WorkTask[];
}
