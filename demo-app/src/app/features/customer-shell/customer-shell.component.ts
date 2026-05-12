import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { LoginStore } from '../login/login.store';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-customer-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatIconModule, MatTooltipModule],
  templateUrl: './customer-shell.component.html',
  styleUrl: './customer-shell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomerShellComponent {
  private readonly router     = inject(Router);
  private readonly loginStore = inject(LoginStore);

  protected readonly navItems: NavItem[] = [
    { label: 'Home',     icon: 'home',         route: '/customer' },
    { label: 'Products', icon: 'storefront',   route: '/customer/products' },
    { label: 'Orders',   icon: 'receipt_long', route: '/customer/orders' },
  ];

  protected logout(): void {
    this.loginStore.logout();
    void this.router.navigate(['/login']);
  }
}
