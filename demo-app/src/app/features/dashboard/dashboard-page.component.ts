import {
  ChangeDetectionStrategy, Component, OnInit, computed, inject, signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule } from '@angular/material/core';
import { OrderMapComponent } from './order-map/order-map.component';
import { DeliveryCalendarComponent } from './delivery-calendar/delivery-calendar.component';

const API = 'http://localhost:8080';

export interface KpiData {
  totalRevenue: number;
  totalOrders: number;
  avgOrderValue: number;
  totalCustomers: number;
}

export interface RevenuePoint { date: string; revenue: number; }
export interface ForecastPoint { date: string; predicted: number; lower: number; upper: number; }
export interface ForecastResponse {
  predictions: ForecastPoint[];
  model?: { type: string; order: string; aic: number; rmse: number; mae: number; training_points: number; };
  error?: string;
}
export interface RecentOrder {
  id: string; personName: string; status: string;
  paymentStatus: string; amount: number; orderDate: string;
}

@Component({
  selector: 'app-dashboard-page',
  imports: [
    MatIconModule, MatButtonModule, MatDatepickerModule,
    MatFormFieldModule, MatInputModule, MatNativeDateModule,
    DecimalPipe, FormsModule,
    OrderMapComponent, DeliveryCalendarComponent,
  ],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardPageComponent implements OnInit {
  private readonly http = inject(HttpClient);

  protected readonly today = new Date();

  protected readonly periods: { label: string; value: 'daily' | 'weekly' | 'monthly' }[] = [
    { label: 'D', value: 'daily' },
    { label: 'W', value: 'weekly' },
    { label: 'M', value: 'monthly' },
  ];

  protected readonly dateFrom = signal<Date>(
    new Date(this.today.getFullYear(), this.today.getMonth() - 3, this.today.getDate()),
  );
  protected readonly dateTo = signal<Date>(this.today);

  protected readonly kpi             = signal<KpiData | null>(null);
  protected readonly revenueData     = signal<RevenuePoint[]>([]);
  protected readonly forecastData    = signal<ForecastPoint[]>([]);
  protected readonly forecastModel   = signal<ForecastResponse['model'] | null>(null);
  protected readonly recentOrders    = signal<RecentOrder[]>([]);
  protected readonly statusBreakdown = signal<Record<string, number>>({});
  protected readonly isLoading       = signal(true);
  protected readonly selectedPeriod  = signal<'daily' | 'weekly' | 'monthly'>('weekly');

  protected readonly totalRevenueFormatted = computed(() => {
    const v = this.kpi()?.totalRevenue ?? 0;
    return new Intl.NumberFormat('ro-RO', {
      style: 'currency', currency: 'RON', maximumFractionDigits: 0,
    }).format(v);
  });

  protected readonly avgOrderFormatted = computed(() => {
    const v = this.kpi()?.avgOrderValue ?? 0;
    return new Intl.NumberFormat('ro-RO', {
      style: 'currency', currency: 'RON', minimumFractionDigits: 2,
    }).format(v);
  });

  // Single line chart, no gradient, no band
  protected readonly chartPath = computed(() => {
    const history  = this.revenueData();
    const forecast = this.forecastData();
    if (history.length < 2) return { line: '', fLine: '' };

    const allValues = [
      ...history.map(d => d.revenue),
      ...forecast.map(f => f.predicted),
    ];
    const min = Math.min(...allValues);
    const max = Math.max(...allValues);
    const range = max - min || 1;

    const W_HIST = 230, W_FORECAST = 90, H = 110;
    const toY = (v: number) => H - ((v - min) / range) * (H - 20) - 10;

    const histPts = history.map((d, i) => ({
      x: (i / Math.max(1, history.length - 1)) * W_HIST,
      y: toY(d.revenue),
    }));

    let line = `M${histPts[0].x.toFixed(1)} ${histPts[0].y.toFixed(1)}`;
    for (let i = 1; i < histPts.length; i++) {
      line += ` L${histPts[i].x.toFixed(1)} ${histPts[i].y.toFixed(1)}`;
    }

    if (forecast.length === 0) return { line, fLine: '' };

    const fPts = forecast.map((f, i) => ({
      x: W_HIST + (i / Math.max(1, forecast.length - 1)) * W_FORECAST,
      y: toY(f.predicted),
    }));
    const startX = histPts[histPts.length - 1].x;
    const startY = histPts[histPts.length - 1].y;
    let fLine = `M${startX.toFixed(1)} ${startY.toFixed(1)}`;
    for (const p of fPts) fLine += ` L${p.x.toFixed(1)} ${p.y.toFixed(1)}`;

    return { line, fLine };
  });

  protected readonly statusEntries = computed(() =>
    Object.entries(this.statusBreakdown()).map(([status, count]) => ({ status, count })),
  );

  ngOnInit(): void { this.loadAll(); }

  protected setPeriod(p: 'daily' | 'weekly' | 'monthly'): void {
    this.selectedPeriod.set(p);
    this.loadRevenue();
  }

  protected applyDateRange(): void { this.loadAll(); }

  protected statusClass(status: string): string {
    return `status-badge status-${(status ?? '').toLowerCase()}`;
  }

  private loadAll(): void {
    this.loadKpi();
    this.loadRevenue();
    this.loadRecentOrders();
    this.loadStatusBreakdown();
    this.loadForecast();
  }

  private dateParams(): HttpParams {
    let params = new HttpParams();
    const from = this.dateFrom(), to = this.dateTo();
    if (from) params = params.set('from', this.toIso(from));
    if (to)   params = params.set('to',   this.toIso(to));
    return params;
  }

  private toIso(d: Date): string { return d.toISOString().substring(0, 10); }

  private loadKpi(): void {
    this.http.get<KpiData>(`${API}/analytics/kpi`, { params: this.dateParams() })
      .subscribe({ next: data => this.kpi.set(data), error: () => {} });
  }

  private loadRevenue(): void {
    const params = this.dateParams().set('period', this.selectedPeriod());
    this.http.get<RevenuePoint[]>(`${API}/analytics/revenue`, { params })
      .subscribe({
        next: data => { this.revenueData.set(data); this.isLoading.set(false); },
        error: () => this.isLoading.set(false),
      });
  }

  private loadForecast(): void {
    this.http.get<ForecastResponse>(`${API}/analytics/forecast?days=14`).subscribe({
      next: (res) => {
        if (res?.predictions) {
          this.forecastData.set(res.predictions);
          this.forecastModel.set(res.model ?? null);
        }
      },
      error: () => this.forecastData.set([]),
    });
  }

  private loadRecentOrders(): void {
    this.http.get<RecentOrder[]>(`${API}/analytics/recent-orders`)
      .subscribe({ next: data => this.recentOrders.set(data), error: () => {} });
  }

  private loadStatusBreakdown(): void {
    this.http.get<Record<string, number>>(`${API}/analytics/order-status-breakdown`)
      .subscribe({ next: data => this.statusBreakdown.set(data), error: () => {} });
  }
}
