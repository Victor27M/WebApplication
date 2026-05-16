import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { LowerCasePipe } from '@angular/common';

const STEPS = ['PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED'] as const;

@Component({
  selector: 'app-order-status-steps',
  imports: [LowerCasePipe],
  templateUrl: './order-status-steps.component.html',
  styleUrl: './order-status-steps.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderStatusStepsComponent {
  readonly status = input.required<string>();

  protected readonly steps = STEPS;

  protected readonly isCancelled = computed(() => this.status() === 'CANCELLED');

  protected readonly currentIndex = computed(() => {
    const idx = STEPS.indexOf(this.status() as typeof STEPS[number]);
    return idx >= 0 ? idx : -1;
  });

  protected stepState(index: number): 'done' | 'current' | 'future' {
    const ci = this.currentIndex();
    if (index < ci) return 'done';
    if (index === ci) return 'current';
    return 'future';
  }
}
