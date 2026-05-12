import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Product } from '../../models/product.model';
import { ProductService } from '../../services/product.service';

@Component({
  selector: 'app-customer-products-page',
  standalone: true,
  imports: [
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    FormsModule, DecimalPipe,
  ],
  templateUrl: './customer-products-page.component.html',
  styleUrl: './customer-products-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomerProductsPageComponent implements OnInit {
  private readonly productService = inject(ProductService);

  protected readonly allProducts = signal<Product[]>([]);
  protected readonly isLoading   = signal(true);
  protected readonly hasError    = signal(false);
  protected readonly search      = signal('');

  protected readonly filteredProducts = computed(() => {
    const q = this.search().toLowerCase().trim();
    return q
      ? this.allProducts().filter(p => p.name.toLowerCase().includes(q))
      : this.allProducts();
  });

  ngOnInit(): void {
    this.productService.getAll().subscribe({
      next: (data) => { this.allProducts.set(data); this.isLoading.set(false); },
      error: () => { this.hasError.set(true); this.isLoading.set(false); },
    });
  }
}
