import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';

@Component({
  selector: 'app-customer-page',
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './customer-page.component.html',
  styleUrl: './customer-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomerPageComponent {
  private readonly router = inject(Router);

  protected goToProducts(): void {
    void this.router.navigate(['/customer/products']);
  }

  protected goToOrders(): void {
    void this.router.navigate(['/customer/orders']);
  }
}
