import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatToolbar } from '@angular/material/toolbar';
import { Router } from '@angular/router';
import { Order, CreateOrderDto } from '../../models/order.model';
import { Product } from '../../models/product.model';
import { OrderService } from '../../services/order.service';
import { ProductService } from '../../services/product.service';
import { LoginStore } from '../login/login.store';
import {
  OrderFormDialogComponent,
  OrderFormDialogData,
  OrderFormDialogResult,
} from '../../components/order-form-dialog/order-form-dialog.component';

@Component({
  selector: 'app-customer-orders-page',
  imports: [MatToolbar, MatButtonModule, MatIconModule, MatTableModule, MatDialogModule],
  templateUrl: './customer-orders-page.component.html',
  styleUrl: './customer-orders-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomerOrdersPageComponent implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly productService = inject(ProductService);
  private readonly loginStore = inject(LoginStore);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);

  protected readonly orders = signal<Order[]>([]);
  protected readonly products = signal<Product[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly hasError = signal(false);
  protected readonly displayedColumns = ['products', 'destination', 'status', 'orderDate'];

  ngOnInit(): void {
    const personId = this.loginStore.personId();
    if (!personId) return;

    this.isLoading.set(true);
    this.orderService.getByPersonId(personId).subscribe({
      next: (data) => {
        this.orders.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.hasError.set(true);
        this.isLoading.set(false);
      },
    });

    this.productService.getAll().subscribe({
      next: (data) => this.products.set(data),
    });
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
            showStatusField: false,
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
          next: (created) => this.orders.update((list) => [...list, created]),
          error: () => this.hasError.set(true),
        });
      });
  }

  protected formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('ro-RO');
  }

  protected goBack(): void {
    void this.router.navigate(['/customer']);
  }

  protected goToProducts(): void {
    void this.router.navigate(['/customer/products']);
  }

  protected logout(): void {
    this.loginStore.logout();
    void this.router.navigate(['/login']);
  }
}
