import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { LoginStore } from '../login/login.store';

interface NavItem {
  label: string;
  icon:  string;
  route: string;
}

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatIconModule, MatTooltipModule],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShellComponent {
  private readonly router     = inject(Router);
  private readonly loginStore = inject(LoginStore);

  protected readonly navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard',     route: '/admin/dashboard' },
    { label: 'Orders',    icon: 'receipt_long',  route: '/admin/orders'    },
    { label: 'Products',  icon: 'inventory_2',   route: '/admin/products'  },
    { label: 'People',    icon: 'people',         route: '/admin/people'    },
  ];

  protected logout(): void {
    this.loginStore.logout();
    void this.router.navigate(['/login']);
  }
}
