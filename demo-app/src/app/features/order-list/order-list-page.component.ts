import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatToolbar } from '@angular/material/toolbar';
import { MatChipsModule } from '@angular/material/chips';
import { Router } from '@angular/router';
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
import { LoginStore } from '../login/login.store';
import { OrderListStore } from './order-list.store';

@Component({
  selector: 'app-order-list-page',
  imports: [
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatToolbar,
    MatChipsModule,
  ],
  templateUrl: './order-list-page.component.html',
  styleUrl: './order-list-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderListPageComponent implements OnInit {
  private readonly dialog = inject(MatDialog);
  private readonly store = inject(OrderListStore);
  private readonly loginStore = inject(LoginStore);
  private readonly personService = inject(PersonService);
  private readonly productService = inject(ProductService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly hasError = this.store.hasError;
  protected readonly isLoading = this.store.isLoading;
  protected readonly orders = this.store.orders;
  protected readonly displayedColumns = [
    'person',
    'products',
    'destination',
    'status',
    'orderDate',
    'actions',
  ];

  protected readonly persons = signal<Person[]>([]);
  protected readonly products = signal<Product[]>([]);

  ngOnInit(): void {
    this.store.load();

    this.personService
      .getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((data) => this.persons.set(data));

    this.productService
      .getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((data) => this.products.set(data));
  }

  protected logout(): void {
    this.loginStore.logout();
    void this.router.navigate(['/login']);
  }

  protected goToPeople(): void {
    void this.router.navigate(['/admin/people']);
  }

  protected goToProducts(): void {
    void this.router.navigate(['/admin/products']);
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
            persons: this.persons(),
            products: this.products(),
          },
        },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (!result) return;
        const dto: CreateOrderDto = {
          personId: result.personId,
          items: result.items,
          destination: result.destination,
          status: result.status,
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
            persons: this.persons(),
            products: this.products(),
            initialValue: {
              personId: order.person?.id,
              items: order.items.map((i) => ({
                productId: i.product.id,
                quantity: i.quantity,
              })),
              destination: order.destination,
              status: order.status,
            },
          },
        },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (!result) return;
        const dto: UpdateOrderDto = {
          personId: result.personId,
          items: result.items,
          destination: result.destination,
          status: result.status,
        };
        this.store.update(order.id, dto);
      });
  }

  protected deleteOrder(order: Order): void {
    if (this.isLoading()) return;

    const confirmed = window.confirm(
      `Are you sure you want to delete this order for "${order.person?.name ?? 'this person'}"?`,
    );
    if (confirmed) {
      this.store.remove(order.id);
    }
  }

  protected getProductNames(order: Order): string {
    return order.items.map((i) => `${i.product.name} x${i.quantity}`).join(', ');
  }

  protected formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('ro-RO');
  }

  protected getPersonName(order: Order): string {
    return order.person?.name ?? '—';
  }

  protected getPersonEmail(order: Order): string {
    return order.person?.email ?? '';
  }
}
