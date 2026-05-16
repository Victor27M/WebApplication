import {
  ChangeDetectionStrategy, Component, computed, DestroyRef,
  effect, inject, OnInit, signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
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
import { CreateOrderDto, Order, OrderItem, UpdateOrderDto } from '../../models/order.model';
import { PersonService } from '../../services/person.service';
import { ProductService } from '../../services/product.service';
import { PaymentService } from '../../services/payment.service';
import { OrderListStore } from './order-list.store';

@Component({
  selector: 'app-order-list-page',
  imports: [
    MatTableModule, MatButtonModule, MatIconModule, MatPaginatorModule,
    MatDialogModule, MatSnackBarModule, MatTooltipModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    FormsModule, DatePipe,
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

  protected readonly isLoading = this.store.isLoading;
  protected readonly hasError  = this.store.hasError;

  protected readonly displayedColumns = [
    'person', 'products', 'destination', 'status', 'paymentStatus', 'date', 'actions',
  ];

  protected readonly availablePersons  = signal<Person[]>([]);
  protected readonly availableProducts = signal<Product[]>([]);

  // ── Filters ─────────────────────────────────────────────────────────────
  protected readonly searchText   = signal('');
  protected readonly filterStatus = signal('ALL');

  protected readonly statusOptions = ['ALL', 'PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'];

  // ── Pagination ──────────────────────────────────────────────────────────
  protected readonly pageIndex = signal(0);
  protected readonly pageSize  = signal(25);

  /** Filtered orders (search + status) */
  protected readonly filteredOrders = computed(() => {
    let list = this.store.orders();
    const q  = this.searchText().toLowerCase().trim();
    const st = this.filterStatus();

    if (q) {
      list = list.filter(o =>
        (o.person?.name ?? '').toLowerCase().includes(q) ||
        (o.person?.email ?? '').toLowerCase().includes(q) ||
        (o.destination ?? '').toLowerCase().includes(q) ||
        (o.id ?? '').toLowerCase().includes(q),
      );
    }

    if (st !== 'ALL') {
      list = list.filter(o => o.status === st);
    }

    return list;
  });

  /** Total count for paginator */
  protected readonly totalOrders = computed(() => this.filteredOrders().length);

  /** Current page slice */
  protected readonly pagedOrders = computed(() => {
    const start = this.pageIndex() * this.pageSize();
    return this.filteredOrders().slice(start, start + this.pageSize());
  });

  constructor() {
    // Reset to page 0 whenever a filter changes
    effect(() => {
      this.searchText();
      this.filterStatus();
      this.pageIndex.set(0);
    });
  }

  ngOnInit(): void {
    this.store.load();
    this.personService.getAll().subscribe(p  => this.availablePersons.set(p));
    this.productService.getAll().subscribe(p => this.availableProducts.set(p));
  }

  protected onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
  }

  // ── Helpers ─────────────────────────────────────────────────────────────

  protected getItems(order: Order): OrderItem[] {
    return order.items ?? [];
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
        next: () => {
          this.store.load();
          this.snackBar.open('Payment successful ✓', 'Close', { duration: 3000 });
        },
        error: () => this.snackBar.open('Payment failed', 'Close', { duration: 3000 }),
      });
  }

  protected refund(order: Order): void {
    this.paymentService.refund(order.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.store.load();
          this.snackBar.open('Refund issued ✓', 'Close', { duration: 3000 });
        },
        error: () => this.snackBar.open('Refund failed', 'Close', { duration: 3000 }),
      });
  }

  // ── Dialogs ─────────────────────────────────────────────────────────────

  protected openCreateDialog(): void {
    if (this.isLoading()) return;
    this.dialog
      .open<OrderFormDialogComponent, OrderFormDialogData, OrderFormDialogResult>(
        OrderFormDialogComponent,
        { data: { title: 'Create Order', submitLabel: 'Create', persons: this.availablePersons(), products: this.availableProducts() } },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (!result) return;
        const dto: CreateOrderDto = {
          personId: result.personId, items: result.items,
          destination: result.destination, status: result.status,
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
            title: 'Edit Order', submitLabel: 'Save',
            persons: this.availablePersons(),
            products: this.availableProducts(),
            initialValue: {
              personId:    order.person?.id ?? '',
              items:       (order.items ?? []).map(i => ({
                productId: i.product?.id ?? '',
                quantity:  i.quantity,
              })),
              destination: order.destination ?? '',
              status:      order.status,
            },
          },
        },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (!result) return;
        const dto: UpdateOrderDto = {
          personId: result.personId, items: result.items,
          destination: result.destination, status: result.status,
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
      .subscribe((confirmed) => { if (confirmed) this.store.remove(order.id); });
  }
}
