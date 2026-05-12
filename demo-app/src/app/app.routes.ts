import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },

  {
    path: 'login',
    loadComponent: () =>
      import('./features/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./features/forgot-password/forgot-password-page.component').then(
        (m) => m.ForgotPasswordPageComponent,
      ),
  },

  // ── Admin area — wrapped in ShellComponent (sidebar + main) ───────────────
  {
    path: 'admin',
    loadComponent: () =>
      import('./features/shell/shell.component').then((m) => m.ShellComponent),
    canActivateChild: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard-page.component').then(
            (m) => m.DashboardPageComponent,
          ),
      },
      {
        path: 'people',
        loadComponent: () =>
          import('./features/person-list/person-list-page.component').then(
            (m) => m.PersonListPageComponent,
          ),
      },
      {
        path: 'products',
        loadComponent: () =>
          import('./features/product-list/product-list-page.component').then(
            (m) => m.ProductListPageComponent,
          ),
      },
      {
        path: 'orders',
        loadComponent: () =>
          import('./features/order-list/order-list-page.component').then(
            (m) => m.OrderListPageComponent,
          ),
      },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
    ],
  },

  // ── Customer area — wrapped in CustomerShellComponent ─────────────────────
  {
    path: 'customer',
    loadComponent: () =>
      import('./features/customer-shell/customer-shell.component').then(
        (m) => m.CustomerShellComponent,
      ),
    canActivateChild: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/customer/customer-page.component').then(
            (m) => m.CustomerPageComponent,
          ),
      },
      {
        path: 'orders',
        loadComponent: () =>
          import('./features/customer-orders/customer-orders-page.component').then(
            (m) => m.CustomerOrdersPageComponent,
          ),
      },
      {
        path: 'products',
        loadComponent: () =>
          import('./features/customer-products/customer-products-page.component').then(
            (m) => m.CustomerProductsPageComponent,
          ),
      },
    ],
  },

  {
    path: 'error',
    loadComponent: () =>
      import('./features/not-found/not-found-page.component').then(
        (m) => m.NotFoundPageComponent,
      ),
  },
  { path: '**', redirectTo: '/error' },
];
