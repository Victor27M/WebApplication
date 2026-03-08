import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { LoginStore } from '../features/login/login.store';

export const authGuard: CanActivateFn = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  const loginStore = inject(LoginStore);
  const router = inject(Router);

  if (!loginStore.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  const role = loginStore.role();
  const fullUrl = state.url;

  if (fullUrl.startsWith('/admin') && role !== 'ADMIN') {
    return router.createUrlTree(['/customer']);
  }

  if (fullUrl.startsWith('/customer') && role !== 'CUSTOMER' && role !== 'ADMIN') {
    return router.createUrlTree(['/login']);
  }

  return true;
};
