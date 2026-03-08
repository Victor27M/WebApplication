import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

const API = 'http://localhost:8080/password-reset';

@Component({
  selector: 'app-forgot-password-page',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    RouterLink,
  ],
  templateUrl: './forgot-password-page.component.html',
  styleUrl: './forgot-password-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ForgotPasswordPageComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  readonly step = signal<'request' | 'confirm' | 'done'>('request');
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Step 1 — enter email
  readonly requestForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  // Step 2 — enter code + new password (twice)
  readonly confirmForm = this.fb.group(
    {
      code: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    },
  );

  protected submitRequest(): void {
    if (this.requestForm.invalid || this.isSubmitting()) {
      this.requestForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const { email } = this.requestForm.getRawValue();

    this.http.post<{ message: string }>(`${API}/request`, { email }).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.step.set('confirm');
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err?.error?.error ?? 'Could not send reset code. Check the email address.');
      },
    });
  }

  protected submitConfirm(): void {
    if (this.confirmForm.invalid || this.isSubmitting()) {
      this.confirmForm.markAllAsTouched();
      return;
    }

    const { newPassword, confirmPassword, code } = this.confirmForm.getRawValue();

    if (newPassword !== confirmPassword) {
      this.errorMessage.set('Passwords do not match.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const email = this.requestForm.getRawValue().email;

    this.http.post<{ message: string }>(`${API}/confirm`, { email, code, newPassword }).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.step.set('done');
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err?.error?.error ?? 'Invalid or expired code.');
      },
    });
  }

  protected goToLogin(): void {
    void this.router.navigate(['/login']);
  }
}
