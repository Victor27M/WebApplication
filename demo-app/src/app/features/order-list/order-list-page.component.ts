import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ConfirmDeleteDialogComponent } from '../../components/confirm-delete-dialog/confirm-delete-dialog.component';
import {
  OrderFormDialogComponent,
  OrderFormDialogData,
  OrderFormDialogResult,
} from '../../components/order-form-dialog/order-form-dialog.component';
import { Person } from '../../models/person.model';
import { Product } from '../../models/product.model';
import { CreateOrderDto, Order, UpdateOrderDto } from '../../models/order.model';
import { PersonService } from '../../services/person.service';
import { ProductService } from '../../services/product.service';
import { PaymentService } from '../../services/payment.service';
import { OrderListStore } from './order-list.store';

@Component({
  selector: 'app-order-list-page',
  standalone: true,
  imports: [
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTooltipModule,
    DatePipe,
  ],
  templateUrl: './order-list-page.component.html',
  styleUrl: './order-list-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderListPageComponent implements OnInit {
  private readonly dialog         = inject(MatDialog);
  private readonly store          = inject(OrderListStore);
  private readonly personService  = inject(PersonService);
  private readonly productService = inject(ProductService);
  private readonly paymentService = inject(PaymentService);
  private readonly snackBar       = inject(MatSnackBar);
  private readonly destroyRef     = inject(DestroyRef);

  protected readonly orders    = this.store.orders;
  protected readonly isLoading = this.store.isLoading;
  protected readonly hasError  = this.store.hasError;

  protected readonly displayedColumns = [
    'person', 'products', 'destination', 'status', 'paymentStatus', 'date', 'actions',
  ];

  protected readonly availablePersons  = signal<Person[]>([]);
  protected readonly availableProducts = signal<Product[]>([]);

  ngOnInit(): void {
    this.store.load();
    this.personService.getAll().subscribe(p  => this.availablePersons.set(p));
    this.productService.getAll().subscribe(p => this.availableProducts.set(p));
  }

  protected getPersonName(order: Order): string  { return order.person?.name  ?? '—'; }
  protected getPersonEmail(order: Order): string { return order.person?.email ?? '';  }

  protected statusClass(status: string): string {
    return `status-badge status-${(status ?? '').toLowerCase()}`;
  }

  protected canPay(order: Order): boolean {
    return order.paymentStatus === 'UNPAID' || order.paymentStatus === 'FAILED';
  }

  protected canRefund(order: Order): boolean {
    return order.paymentStatus === 'PAID';
  }

  protected pay(order: Order): void {
    this.paymentService.pay(order.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.store.orders.update(list =>
            list.map(o => o.id === updated.id ? { ...o, paymentStatus: updated.paymentStatus } : o),
          );
          this.snackBar.open('Payment successful ✓', 'Close', { duration: 3000 });
        },
        error: () => this.snackBar.open('Payment failed', 'Close', { duration: 3000 }),
      });
  }

  protected refund(order: Order): void {
    this.paymentService.refund(order.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.store.orders.update(list =>
            list.map(o => o.id === updated.id ? { ...o, paymentStatus: updated.paymentStatus } : o),
          );
          this.snackBar.open('Refund issued ✓', 'Close', { duration: 3000 });
        },
        error: () => this.snackBar.open('Refund failed', 'Close', { duration: 3000 }),
      });
  }

  protected openCreateDialog(): void {
    if (this.isLoading()) return;
    this.dialog
      .open<OrderFormDialogComponent, OrderFormDialogData, OrderFormDialogResult>(
        OrderFormDialogComponent,
        {
          data: {
            title: 'Create Order',
            submitLabel: 'Create',
            persons: this.availablePersons(),
            products: this.availableProducts(),
          },
        },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (!result) return;
        const dto: CreateOrderDto = {
          personId:    result.personId,
          items:       result.items,
          destination: result.destination,
          status:      result.status,
        };
        this.store.create(dto);
      });
  }

  protected openEditDialog(order: Order): void {
    if (this.isLoading()) return;
    this.dialog
      .open<OrderFormDialogComponent, OrderFormDialogData, OrderFormDialogResult>(
        OrderFormDialogComponent,
        {
          data: {
            title: 'Edit Order',
            submitLabel: 'Save',
            persons: this.availablePersons(),
            products: this.availableProducts(),
            initialValue: order,
          },
        },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (!result) return;
        const dto: UpdateOrderDto = {
          personId:    result.personId,
          items:       result.items,
          destination: result.destination,
          status:      result.status,
        };
        this.store.update(order.id, dto);
      });
  }

  protected openDeleteDialog(order: Order): void {
    if (this.isLoading()) return;
    this.dialog
      .open(ConfirmDeleteDialogComponent, {
        data: { name: `Order #${order.id?.toString().slice(0, 8)}` },
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((confirmed) => {
        if (confirmed) this.store.remove(order.id);
      });
  }
}
