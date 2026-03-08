import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateOrderDto, Order, UpdateOrderDto } from '../models/order.model';

const API_URL = 'http://localhost:8080/order';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<Order[]> {
    return this.http.get<Order[]>(API_URL);
  }

  getByPersonId(personId: string): Observable<Order[]> {
    return this.http.get<Order[]>(`${API_URL}/person/${personId}`);
  }

  create(dto: CreateOrderDto): Observable<Order> {
    return this.http.post<Order>(API_URL, dto);
  }

  update(id: string, dto: UpdateOrderDto): Observable<Order> {
    return this.http.put<Order>(`${API_URL}/${id}`, dto);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${API_URL}/${id}`);
  }
}
