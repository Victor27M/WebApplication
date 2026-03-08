import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  signal,
} from '@angular/core';
import { DecimalPipe, CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatToolbar } from '@angular/material/toolbar';
import { Router } from '@angular/router';
import {
  ProductFormDialogComponent,
  ProductFormDialogData,
  ProductFormDialogResult,
} from '../../components/product-form-dialog/product-form-dialog.component';
import { CreateProductDto, Product, UpdateProductDto } from '../../models/product.model';
import { LoginStore } from '../login/login.store';
import { ProductListStore } from './product-list.store';

type SortOption = 'name-asc' | 'name-desc' | 'price-asc' | 'price-desc';

@Component({
  selector: 'app-product-list-page',
  imports: [
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatToolbar,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    FormsModule,
    DecimalPipe,
  ],
  templateUrl: './product-list-page.component.html',
  styleUrl: './product-list-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductListPageComponent {
  private readonly dialog = inject(MatDialog);
  private readonly store = inject(ProductListStore);
  private readonly loginStore = inject(LoginStore);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly hasError = this.store.hasError;
  protected readonly isLoading = this.store.isLoading;
  protected readonly displayedColumns = ['name', 'description', 'price', 'stock', 'actions'];

  // ── filter & sort signals ───────────────────────────────────────────────────
  protected readonly filterName = signal('');
  protected readonly filterMaxPrice = signal<number | null>(null);
  protected readonly sortOption = signal<SortOption>('name-asc');

  protected readonly filteredProducts = computed(() => {
    const name = this.filterName().toLowerCase().trim();
    const maxPrice = this.filterMaxPrice();
    const sort = this.sortOption();

    let list = this.store.products();

    if (name) {
      list = list.filter((p) => p.name.toLowerCase().includes(name));
    }

    if (maxPrice !== null && maxPrice >= 0) {
      list = list.filter((p) => p.price <= maxPrice);
    }

    return [...list].sort((a, b) => {
      switch (sort) {
        case 'name-asc':  return a.name.localeCompare(b.name);
        case 'name-desc': return b.name.localeCompare(a.name);
        case 'price-asc': return a.price - b.price;
        case 'price-desc': return b.price - a.price;
      }
    });
  });

  constructor() {
    this.store.load();
  }

  protected logout(): void {
    this.loginStore.logout();
    void this.router.navigate(['/login']);
  }

  protected goToPeople(): void {
    void this.router.navigate(['/admin/people']);
  }

  protected goToOrders(): void {
    void this.router.navigate(['/admin/orders']);
  }

  protected openCreateDialog(): void {
    if (this.isLoading()) return;

    this.dialog
      .open<ProductFormDialogComponent, ProductFormDialogData, ProductFormDialogResult>(
        ProductFormDialogComponent,
        { data: { title: 'Create Product', submitLabel: 'Create' } },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (!result) return;
        this.store.create(result as CreateProductDto);
      });
  }

  protected openEditDialog(product: Product): void {
    if (this.isLoading()) return;

    this.dialog
      .open<ProductFormDialogComponent, ProductFormDialogData, ProductFormDialogResult>(
        ProductFormDialogComponent,
        {
          data: {
            title: 'Edit Product',
            submitLabel: 'Save',
            initialValue: product,
          },
        },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (!result) return;
        this.store.update(product.id, result as UpdateProductDto);
      });
  }

  protected openDeleteDialog(product: Product): void {
    if (this.isLoading()) return;

    const confirmed = window.confirm(
      `Are you sure you want to delete "${product.name}"?`,
    );
    if (confirmed) {
      this.store.remove(product.id);
    }
  }
}
