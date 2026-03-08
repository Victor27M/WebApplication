import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatToolbar } from '@angular/material/toolbar';
import { Router } from '@angular/router';
import { Product } from '../../models/product.model';
import { ProductService } from '../../services/product.service';
import { LoginStore } from '../login/login.store';

@Component({
  selector: 'app-customer-products-page',
  imports: [MatToolbar, MatButtonModule, MatIconModule, MatTableModule, DecimalPipe],
  templateUrl: './customer-products-page.component.html',
  styleUrl: './customer-products-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomerProductsPageComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly loginStore = inject(LoginStore);
  private readonly router = inject(Router);

  protected readonly products = signal<Product[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly hasError = signal(false);
  protected readonly displayedColumns = ['name', 'description', 'price', 'stock'];

  ngOnInit(): void {
    this.isLoading.set(true);
    this.productService.getAll().subscribe({
      next: (data) => {
        this.products.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.hasError.set(true);
        this.isLoading.set(false);
      },
    });
  }

  protected goBack(): void {
    void this.router.navigate(['/customer']);
  }

  protected goToOrders(): void {
    void this.router.navigate(['/customer/orders']);
  }

  protected logout(): void {
    this.loginStore.logout();
    void this.router.navigate(['/login']);
  }
}
