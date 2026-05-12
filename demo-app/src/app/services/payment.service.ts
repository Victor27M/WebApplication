import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Order } from '../models/order.model';

const API = 'http://localhost:8080';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);

  /**
   * Triggers payment for an order (UNPAID → PAID via State pattern).
   * Returns the updated Order or throws 409 if the transition is illegal.
   */
  pay(orderId: string): Observable<Order> {
    return this.http.post<Order>(`${API}/payment/${orderId}/pay`, {});
  }

  /**
   * Issues a refund (PAID → REFUNDED via State pattern).
   * Returns the updated Order or throws 409 if the transition is illegal.
   */
  refund(orderId: string): Observable<Order> {
    return this.http.post<Order>(`${API}/payment/${orderId}/refund`, {});
  }
}
