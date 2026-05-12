import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
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
  standalone: true,
  templateUrl: './delivery-calendar.component.html',
  styleUrl: './delivery-calendar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeliveryCalendarComponent implements OnInit {
  private readonly http = inject(HttpClient);

  protected readonly today        = new Date();
  protected readonly viewDate     = signal(new Date());
  protected readonly allOrders    = signal<Order[]>([]);
  protected readonly selectedDay  = signal<CalendarDay | null>(null);

  protected readonly monthLabel = computed(() =>
    this.viewDate().toLocaleDateString('en-GB', { month: 'long', year: 'numeric' }),
  );

  protected readonly weekHeaders = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  protected readonly calendarDays = computed((): CalendarDay[] => {
    const view   = this.viewDate();
    const orders = this.allOrders();
    const year   = view.getFullYear();
    const month  = view.getMonth();

    // First day of the month (0=Sun … 6=Sat) → shift to Mon-based
    const firstDay   = new Date(year, month, 1);
    const startOffset = (firstDay.getDay() + 6) % 7; // Mon = 0
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    const days: CalendarDay[] = [];

    // Padding days from previous month
    for (let i = startOffset - 1; i >= 0; i--) {
      const d = new Date(year, month, -i);
      days.push({ date: d, dayNum: d.getDate(), isToday: false, isCurrentMonth: false, orders: [] });
    }

    // Current month days
    const todayStr = this.today.toDateString();
    for (let d = 1; d <= daysInMonth; d++) {
      const date    = new Date(year, month, d);
      const dateStr = date.toDateString();

      const dayOrders = orders.filter(o => {
        const od = new Date(o.orderDate);
        return od.toDateString() === dateStr;
      });

      days.push({
        date,
        dayNum: d,
        isToday: dateStr === todayStr,
        isCurrentMonth: true,
        orders: dayOrders,
      });
    }

    // Padding to complete the last week (42 cells total)
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
    this.selectedDay.set(this.selectedDay()?.date.toDateString() === day.date.toDateString() ? null : day);
  }

  protected statusClass(status: string): string {
    return `dot dot-${status.toLowerCase()}`;
  }

  protected dominantStatus(orders: Order[]): string {
    const priorities = ['PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'];
    for (const s of priorities) {
      if (orders.some(o => o.status === s)) return s.toLowerCase();
    }
    return 'pending';
  }
}
