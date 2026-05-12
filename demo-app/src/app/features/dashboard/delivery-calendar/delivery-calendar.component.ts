import {
  ChangeDetectionStrategy, Component, computed,
  inject, OnInit, signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Order } from '../../../models/order.model';

const API = 'http://localhost:8080';

interface CalendarDay {
  date: Date;
  dayNum: number;
  isToday: boolean;
  isCurrentMonth: boolean;
  orders: Order[];
}

@Component({
  selector: 'app-delivery-calendar',
  imports: [DatePipe],
  templateUrl: './delivery-calendar.component.html',
  styleUrl: './delivery-calendar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeliveryCalendarComponent implements OnInit {
  private readonly http = inject(HttpClient);

  protected readonly today       = new Date();
  protected readonly viewDate    = signal(new Date());
  protected readonly allOrders   = signal<Order[]>([]);
  protected readonly selectedDay = signal<CalendarDay | null>(null);

  protected readonly monthLabel = computed(() =>
    this.viewDate().toLocaleDateString('en-GB', { month: 'long', year: 'numeric' }),
  );

  protected readonly weekHeaders = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  protected readonly calendarDays = computed((): CalendarDay[] => {
    const view   = this.viewDate();
    const orders = this.allOrders();
    const year   = view.getFullYear();
    const month  = view.getMonth();

    const firstDay    = new Date(year, month, 1);
    const startOffset = (firstDay.getDay() + 6) % 7;
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const days: CalendarDay[] = [];
    const todayStr = this.today.toDateString();

    for (let i = startOffset - 1; i >= 0; i--) {
      const d = new Date(year, month, -i);
      days.push({ date: d, dayNum: d.getDate(), isToday: false, isCurrentMonth: false, orders: [] });
    }

    for (let d = 1; d <= daysInMonth; d++) {
      const date      = new Date(year, month, d);
      const dateStr   = date.toDateString();
      const dayOrders = orders.filter(o => new Date(o.orderDate).toDateString() === dateStr);
      days.push({ date, dayNum: d, isToday: dateStr === todayStr, isCurrentMonth: true, orders: dayOrders });
    }

    while (days.length < 42) {
      const d = new Date(year, month + 1, days.length - startOffset - daysInMonth + 1);
      days.push({ date: d, dayNum: d.getDate(), isToday: false, isCurrentMonth: false, orders: [] });
    }

    return days;
  });

  ngOnInit(): void {
    this.http.get<Order[]>(`${API}/order`).subscribe({
      next: (orders) => this.allOrders.set(orders),
      error: () => {},
    });
  }

  // Helper avoids optional-chain type error in template
  protected isSelectedDay(day: CalendarDay): boolean {
    const sel = this.selectedDay();
    return sel !== null && sel.date.toDateString() === day.date.toDateString();
  }

  protected prevMonth(): void {
    const v = this.viewDate();
    this.viewDate.set(new Date(v.getFullYear(), v.getMonth() - 1, 1));
    this.selectedDay.set(null);
  }

  protected nextMonth(): void {
    const v = this.viewDate();
    this.viewDate.set(new Date(v.getFullYear(), v.getMonth() + 1, 1));
    this.selectedDay.set(null);
  }

  protected selectDay(day: CalendarDay): void {
    if (!day.isCurrentMonth || day.orders.length === 0) return;
    this.selectedDay.set(this.isSelectedDay(day) ? null : day);
  }

  protected dotClass(status: string): string {
    return `dot dot-${(status ?? '').toLowerCase()}`;
  }
}
