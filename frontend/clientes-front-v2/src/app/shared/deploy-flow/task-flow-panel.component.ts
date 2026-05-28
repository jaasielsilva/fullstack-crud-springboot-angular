import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { environment } from '../../../environments/environment';
import {
  DEPLOY_FLOW_STEPS,
  FLOW_LOCATION_BADGE,
  FLOW_LOCATION_LABEL
} from './deploy-flow.steps';
import { currentDeployTier, suggestCurrentStepIndex } from './deploy-flow.context';
import { ChangeRequest } from '../../models/gmud/change-request.model';
import { WorkTask } from '../../models/task/work-task.model';

@Component({
  selector: 'app-task-flow-panel',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './task-flow-panel.component.html',
  styleUrl: './task-flow-panel.component.css'
})
export class TaskFlowPanelComponent implements OnChanges {
  @Input({ required: true }) task!: WorkTask;
  @Input() gmuds: ChangeRequest[] = [];

  readonly steps = DEPLOY_FLOW_STEPS;
  readonly locationLabel = FLOW_LOCATION_LABEL;
  readonly locationBadge = FLOW_LOCATION_BADGE;
  readonly appTier = currentDeployTier();
  readonly hmlUrl = environment.hmlAppUrl;
  readonly prodUrl = environment.prodAppUrl;

  currentStepIndex = 0;

  ngOnChanges(): void {
    if (this.task) {
      this.currentStepIndex = suggestCurrentStepIndex(this.task, this.gmuds);
    }
  }

  stepState(index: number): 'done' | 'current' | 'pending' {
    if (this.task.status === 'CANCELLED') {
      return 'pending';
    }
    if (index < this.currentStepIndex) {
      return 'done';
    }
    if (index === this.currentStepIndex) {
      return 'current';
    }
    return 'pending';
  }
}
