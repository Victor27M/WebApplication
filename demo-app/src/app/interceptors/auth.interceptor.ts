import {HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';
import {LoginStore} from '../features/login/login.store';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const loginStore = inject(LoginStore);
  const token = loginStore.token();

  if (!token || req.url.includes('/login')) {
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: {Authorization: `Bearer ${token}`},
  });

  return next(authReq);
};
