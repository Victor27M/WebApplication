import {
  ChangeDetectionStrategy, Component,
  inject, OnInit, signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { CreateOrderDto, Order, OrderItem } from '../../models/order.model';
import { Product } from '../../models/product.model';
import { OrderService } from '../../services/order.service';
import { ProductService } from '../../services/product.service';
import { LoginStore } from '../login/login.store';
import {
  OrderFormDialogComponent,
  OrderFormDialogData,
  OrderFormDialogResult,
} from '../../components/order-form-dialog/order-form-dialog.component';
import { OrderStatusStepsComponent } from '../order-status-steps/order-status-steps.component';

@Component({
  selector: 'app-customer-orders-page',
  imports: [
    MatButtonModule, MatIconModule, MatTableModule,
    MatDialogModule, DatePipe, OrderStatusStepsComponent,
  ],
  templateUrl: './customer-orders-page.component.html',
  styleUrl: './customer-orders-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomerOrdersPageComponent implements OnInit {
  private readonly orderService   = inject(OrderService);
  private readonly productService = inject(ProductService);
  private readonly loginStore     = inject(LoginStore);
  private readonly dialog         = inject(MatDialog);

  protected readonly orders    = signal<Order[]>([]);
  protected readonly products  = signal<Product[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly hasError  = signal(false);

  protected readonly displayedColumns = [
    'products', 'destination', 'tracking', 'paymentStatus', 'orderDate',
  ];

  ngOnInit(): void {
    const personId = this.loginStore.personId();
    if (!personId) return;

    this.isLoading.set(true);
    this.orderService.getByPersonId(personId).subscribe({
      next: (data) => { this.orders.set(data); this.isLoading.set(false); },
      error: () => { this.hasError.set(true); this.isLoading.set(false); },
    });
    this.productService.getAll().subscribe({ next: (data) => this.products.set(data) });
  }

  protected getItems(order: Order): OrderItem[] {
    return order.items ?? [];
  }

  protected statusClass(status: string): string {
    return `status-badge status-${(status ?? '').toLowerCase()}`;
  }

  protected openCreateDialog(): void {
    const personId = this.loginStore.personId();
    if (!personId) return;

    this.dialog
      .open<OrderFormDialogComponent, OrderFormDialogData, OrderFormDialogResult>(
        OrderFormDialogComponent,
        {
          data: {
            title: 'New Order',
            submitLabel: 'Place Order',
            persons: [{ id: personId, name: 'Me', email: '', age: 0, password: '', role: 'CUSTOMER' }],
            products: this.products(),
          },
        },
      )
      .afterClosed()
      .subscribe((result) => {
        if (!result) return;
        const dto: CreateOrderDto = {
          personId,
          items: result.items,
          destination: result.destination,
          status: 'PENDING',
        };
        this.orderService.create(dto).subscribe({
          next: (created) => this.orders.update(list => [...list, created]),
          error: () => this.hasError.set(true),
        });
      });
  }
}
