import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatIconModule, MatButtonModule, MatTooltipModule],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShellComponent {
  private readonly router = inject(Router);

  protected readonly isDark = signal(true);

  protected readonly navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'grid_view',      route: '/admin/dashboard' },
    { label: 'Orders',    icon: 'receipt_long',   route: '/admin/orders' },
    { label: 'Products',  icon: 'inventory_2',    route: '/admin/products' },
    { label: 'Persons',   icon: 'people',         route: '/admin/people' },
  ];

  protected toggleTheme(): void {
    this.isDark.update(v => !v);
    document.documentElement.setAttribute(
      'data-theme',
      this.isDark() ? 'dark' : 'light',
    );
  }

  protected logout(): void {
    sessionStorage.clear();
    void this.router.navigate(['/login']);
  }
}

import { inject } from '@angular/core';
