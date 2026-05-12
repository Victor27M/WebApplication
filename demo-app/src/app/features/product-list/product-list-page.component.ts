import {
  ChangeDetectionStrategy, Component, computed,
  DestroyRef, inject, signal,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { ConfirmDeleteDialogComponent } from '../../components/confirm-delete-dialog/confirm-delete-dialog.component';
import {
  ProductFormDialogComponent,
  ProductFormDialogData,
  ProductFormDialogResult,
} from '../../components/product-form-dialog/product-form-dialog.component';
import { CreateProductDto, Product, UpdateProductDto } from '../../models/product.model';
import { ProductListStore } from './product-list.store';

type SortOption = 'name-asc' | 'name-desc' | 'price-asc' | 'price-desc';

@Component({
  selector: 'app-product-list-page',
  imports: [
    MatTableModule, MatButtonModule, MatIconModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, FormsModule, DecimalPipe,
  ],
  templateUrl: './product-list-page.component.html',
  styleUrl: './product-list-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductListPageComponent {
  private readonly dialog     = inject(MatDialog);
  private readonly store      = inject(ProductListStore);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly isLoading        = this.store.isLoading;
  protected readonly hasError         = this.store.hasError;
  protected readonly displayedColumns = ['name', 'description', 'price', 'stock', 'actions'];

  protected readonly filterName     = signal('');
  protected readonly filterMaxPrice = signal<number | null>(null);
  protected readonly sortOption     = signal<SortOption>('name-asc');

  protected readonly filteredProducts = computed(() => {
    let list = this.store.products();
    const name = this.filterName().toLowerCase().trim();
    const max  = this.filterMaxPrice();
    const sort = this.sortOption();
    if (name) list = list.filter(p => p.name.toLowerCase().includes(name));
    if (max !== null) list = list.filter(p => p.price <= max);
    return [...list].sort((a, b) => {
      switch (sort) {
        case 'name-asc':   return a.name.localeCompare(b.name);
        case 'name-desc':  return b.name.localeCompare(a.name);
        case 'price-asc':  return a.price - b.price;
        case 'price-desc': return b.price - a.price;
      }
    });
  });

  constructor() { this.store.load(); }

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
        const dto: CreateProductDto = {
          name: result.name, description: result.description,
          price: result.price, stock: result.stock,
        };
        this.store.create(dto);
      });
  }

  protected openEditDialog(product: Product): void {
    if (this.isLoading()) return;
    this.dialog
      .open<ProductFormDialogComponent, ProductFormDialogData, ProductFormDialogResult>(
        ProductFormDialogComponent,
        { data: { title: 'Edit Product', submitLabel: 'Save', initialValue: product } },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (!result) return;
        const dto: UpdateProductDto = {
          name: result.name, description: result.description,
          price: result.price, stock: result.stock,
        };
        this.store.update(product.id, dto);   // ← two separate args
      });
  }

  protected openDeleteDialog(product: Product): void {
    if (this.isLoading()) return;
    this.dialog
      .open(ConfirmDeleteDialogComponent, { data: { name: product.name } })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((confirmed) => {
        if (confirmed) this.store.remove(product.id);   // ← remove not delete
      });
  }
}
