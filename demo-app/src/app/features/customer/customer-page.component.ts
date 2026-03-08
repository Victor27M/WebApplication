import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbar } from '@angular/material/toolbar';
import { Router } from '@angular/router';
import { LoginStore } from '../login/login.store';

@Component({
  selector: 'app-customer-page',
  imports: [MatToolbar, MatButtonModule, MatIconModule],
  templateUrl: './customer-page.component.html',
  styleUrl: './customer-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomerPageComponent {
  private readonly loginStore = inject(LoginStore);
  private readonly router = inject(Router);

  protected logout(): void {
    this.loginStore.logout();
    void this.router.navigate(['/login']);
  }

  protected goToOrders(): void {
    void this.router.navigate(['/customer/orders']);
  }

  protected goToProducts(): void {
    void this.router.navigate(['/customer/products']);
  }
}
