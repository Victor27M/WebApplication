import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { LoginStore } from './login.store';

@Component({
  selector: 'app-login-page',
  imports: [MatIconModule],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPageComponent {
  private readonly router = inject(Router);
  private readonly loginStore = inject(LoginStore);

  protected readonly email        = signal('');
  protected readonly password     = signal('');
  protected readonly errorMessage = signal('');
  protected readonly isLoading    = signal(false);

  protected onSubmit(event: Event): void {
    event.preventDefault();
    this.errorMessage.set('');

    if (!this.email() || !this.password()) {
      this.errorMessage.set('Please enter both email and password.');
      return;
    }

    this.isLoading.set(true);
    this.loginStore.login({ email: this.email(), password: this.password() }).subscribe({
      next: () => {
        const role = this.loginStore.role();
        this.router.navigate([role === 'ADMIN' ? '/admin/dashboard' : '/customer']);
      },
      error: () => {
        this.errorMessage.set('Invalid email or password.');
        this.isLoading.set(false);
      },
    });
  }

  protected goToForgotPassword(): void {
    void this.router.navigate(['/forgot-password']);
  }
}
