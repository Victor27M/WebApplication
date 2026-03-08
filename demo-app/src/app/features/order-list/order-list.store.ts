import { computed, inject, Injectable, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { CreateOrderDto, Order, UpdateOrderDto } from '../../models/order.model';
import { OrderService } from '../../services/order.service';

@Injectable({ providedIn: 'root' })
export class OrderListStore {
  private readonly orderService = inject(OrderService);
  private readonly pendingRequests = signal(0);

  readonly orders = signal<Order[]>([]);
  readonly hasError = signal(false);
  readonly isLoading = computed(() => this.pendingRequests() > 0);

  private beginRequest(): void {
    this.pendingRequests.update((count) => count + 1);
  }

  private endRequest(): void {
    this.pendingRequests.update((count) => Math.max(0, count - 1));
  }

  load(): void {
    this.hasError.set(false);
    this.beginRequest();
    this.orderService
      .getAll()
      .pipe(finalize(() => this.endRequest()))
      .subscribe({
        next: (data) => this.orders.set(data),
        error: () => this.hasError.set(true),
      });
  }

  create(dto: CreateOrderDto): void {
    this.hasError.set(false);
    this.beginRequest();
    this.orderService
      .create(dto)
      .pipe(finalize(() => this.endRequest()))
      .subscribe({
        next: (created) => this.orders.update((list) => [...list, created]),
        error: () => this.hasError.set(true),
      });
  }

  update(id: string, dto: UpdateOrderDto): void {
    this.hasError.set(false);
    this.beginRequest();
    this.orderService
      .update(id, dto)
      .pipe(finalize(() => this.endRequest()))
      .subscribe({
        next: (updated) =>
          this.orders.update((list) =>
            list.map((o) => (o.id === updated.id ? updated : o)),
          ),
        error: () => this.hasError.set(true),
      });
  }

  remove(id: string): void {
    this.hasError.set(false);
    this.beginRequest();
    this.orderService
      .delete(id)
      .pipe(finalize(() => this.endRequest()))
      .subscribe({
        next: () =>
          this.orders.update((list) => list.filter((o) => o.id !== id)),
        error: () => this.hasError.set(true),
      });
  }
}
