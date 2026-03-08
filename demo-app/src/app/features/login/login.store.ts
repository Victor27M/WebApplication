import { inject, Injectable, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, finalize, Observable, of, tap } from 'rxjs';
import { LoginRequest, LoginResponse, LoginService } from '../../services/login.service';

interface AuthSnapshot {
  isAuthenticated: boolean;
  role: string | null;
  personId: string | null;
  token: string | null;
}

const STORAGE_KEY = 'demo-app-auth';

@Injectable({ providedIn: 'root' })
export class LoginStore {
  private readonly loginService = inject(LoginService);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly isAuthenticated = signal(false);
  readonly role = signal<string | null>(null);
  readonly personId = signal<string | null>(null);
  readonly token = signal<string | null>(null);

  constructor() {
    this.restoreAuthState();
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    this.errorMessage.set(null);
    this.isSubmitting.set(true);

    return this.loginService.login(request).pipe(
      tap((response) => this.applyResponse(response)),
      catchError((error: unknown) => {
        const response = this.normalizeError(error);
        this.applyResponse(response);
        return of(response);
      }),
      finalize(() => this.isSubmitting.set(false)),
    );
  }

  logout(): void {
    this.clearSession();
  }

  private applyResponse(response: LoginResponse): void {
    if (response.success) {
      this.isAuthenticated.set(true);
      this.role.set(response.role);
      this.personId.set(response.personId);
      this.token.set(response.token);
      this.errorMessage.set(null);
      this.persistAuthState();
      return;
    }

    this.clearSession(response.errorMessage ?? 'Login failed. Please try again.');
  }

  private normalizeError(error: unknown): LoginResponse {
    if (error instanceof HttpErrorResponse && error.error) {
      const maybeError = error.error as Partial<LoginResponse>;
      if (typeof maybeError.success === 'boolean') {
        return {
          success: maybeError.success,
          role: maybeError.role ?? null,
          personId: maybeError.personId ?? null,
          token: maybeError.token ?? null,
          errorMessage:
            maybeError.errorMessage ??
            (error.status === 401
              ? 'Invalid email or password.'
              : 'Unable to complete login. Please try again.'),
        };
      }
    }

    return {
      success: false,
      role: null,
      personId: null,
      token: null,
      errorMessage: 'Unable to complete login. Please try again.',
    };
  }

  private restoreAuthState(): void {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    if (!stored) return;

    try {
      const snapshot = JSON.parse(stored) as AuthSnapshot;
      this.isAuthenticated.set(snapshot.isAuthenticated);
      this.role.set(snapshot.role ?? null);
      this.personId.set(snapshot.personId ?? null);
      this.token.set(snapshot.token ?? null);
    } catch {
      this.clearSession();
    }
  }

  private persistAuthState(): void {
    const snapshot: AuthSnapshot = {
      isAuthenticated: this.isAuthenticated(),
      role: this.role(),
      personId: this.personId(),
      token: this.token(),
    };
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(snapshot));
  }

  private clearSession(errorMessage: string | null = null): void {
    this.isAuthenticated.set(false);
    this.role.set(null);
    this.personId.set(null);
    this.token.set(null);
    this.errorMessage.set(errorMessage);
    sessionStorage.removeItem(STORAGE_KEY);
  }
}
