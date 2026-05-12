import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DecimalPipe, DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { OrderMapComponent } from './order-map/order-map.component';

const API = 'http://localhost:8080';

export interface KpiData {
  totalRevenue: number;
  totalOrders: number;
  avgOrderValue: number;
  totalCustomers: number;
}

export interface RevenuePoint {
  date: string;
  revenue: number;
}

export interface ForecastPoint {
  date: string;
  predicted: number;
  lower: number;
  upper: number;
}

export interface ForecastResponse {
  predictions: ForecastPoint[];
  model?: {
    type: string;
    order: string;
    aic: number;
    rmse: number;
    mae: number;
    training_points: number;
  };
  error?: string;
}

export interface RecentOrder {
  id: string;
  personName: string;
  status: string;
  paymentStatus: string;
  amount: number;
  orderDate: string;
}

@Component({
  selector: 'app-dashboard-page',
  imports: [MatIconModule, DecimalPipe, DatePipe, OrderMapComponent],
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

  protected readonly kpi              = signal<KpiData | null>(null);
  protected readonly revenueData      = signal<RevenuePoint[]>([]);
  protected readonly forecastData     = signal<ForecastPoint[]>([]);
  protected readonly forecastModel    = signal<ForecastResponse['model'] | null>(null);
  protected readonly recentOrders     = signal<RecentOrder[]>([]);
  protected readonly statusBreakdown  = signal<Record<string, number>>({});
  protected readonly isLoading        = signal(true);
  protected readonly selectedPeriod   = signal<'daily' | 'weekly' | 'monthly'>('weekly');

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

  protected readonly sparklinePath = computed(() => {
    const data = this.revenueData();
    if (data.length < 2) return '';
    const values = data.map(d => d.revenue);
    const min = Math.min(...values);
    const max = Math.max(...values);
    const range = max - min || 1;
    const points = values.map((v, i) => ({
      x: (i / (values.length - 1)) * 80,
      y: 28 - ((v - min) / range) * 24 - 2,
    }));
    let d = `M${points[0].x.toFixed(1)} ${points[0].y.toFixed(1)}`;
    for (let i = 1; i < points.length; i++) {
      const cp = points[i - 1];
      const np = points[i];
      const mx = (cp.x + np.x) / 2;
      d += ` C${mx.toFixed(1)} ${cp.y.toFixed(1)} ${mx.toFixed(1)} ${np.y.toFixed(1)} ${np.x.toFixed(1)} ${np.y.toFixed(1)}`;
    }
    return d;
  });

  protected readonly sparklineArea = computed(() => {
    const path = this.sparklinePath();
    if (!path) return '';
    return `${path} L80 28 L0 28 Z`;
  });

  /**
   * Builds the SVG paths for the chart, combining historical revenue and the
   * ARIMA forecast on a shared X-axis. The historical part fills 0–230 of the
   * 320 viewBox; the forecast fills 230–320 (dashed, with confidence band).
   */
  protected readonly chartPath = computed(() => {
    const history  = this.revenueData();
    const forecast = this.forecastData();
    if (history.length < 2) return { line: '', area: '', fLine: '', fArea: '', band: '' };

    // Combined min/max so both segments share a Y scale
    const allValues = [
      ...history.map(d => d.revenue),
      ...forecast.flatMap(f => [f.predicted, f.lower, f.upper]),
    ];
    const min = Math.min(...allValues);
    const max = Math.max(...allValues);
    const range = max - min || 1;

    const W_HIST     = 230;
    const W_FORECAST = 90;
    const H = 110;

    const toY = (v: number) => H - ((v - min) / range) * (H - 20) - 10;

    // ── Historical ──
    const histPts = history.map((d, i) => ({
      x: (i / Math.max(1, history.length - 1)) * W_HIST,
      y: toY(d.revenue),
    }));
    let line = `M${histPts[0].x.toFixed(1)} ${histPts[0].y.toFixed(1)}`;
    for (let i = 1; i < histPts.length; i++) {
      const cp = histPts[i - 1];
      const np = histPts[i];
      const mx = (cp.x + np.x) / 2;
      line += ` C${mx.toFixed(1)} ${cp.y.toFixed(1)} ${mx.toFixed(1)} ${np.y.toFixed(1)} ${np.x.toFixed(1)} ${np.y.toFixed(1)}`;
    }
    const area = `${line} L${W_HIST} 130 L0 130 Z`;

    // ── Forecast ──
    if (forecast.length === 0) {
      return { line, area, fLine: '', fArea: '', band: '' };
    }

    const fPts = forecast.map((f, i) => ({
      x: W_HIST + (i / Math.max(1, forecast.length - 1)) * W_FORECAST,
      y: toY(f.predicted),
      yL: toY(f.lower),
      yU: toY(f.upper),
    }));

    // Predicted line starts from the last historical point
    const startX = histPts[histPts.length - 1].x;
    const startY = histPts[histPts.length - 1].y;
    let fLine = `M${startX.toFixed(1)} ${startY.toFixed(1)}`;
    for (const p of fPts) {
      fLine += ` L${p.x.toFixed(1)} ${p.y.toFixed(1)}`;
    }

    // Confidence band (upper → reverse lower)
    let bandUp   = `M${startX.toFixed(1)} ${startY.toFixed(1)}`;
    let bandDown = '';
    for (const p of fPts) {
      bandUp += ` L${p.x.toFixed(1)} ${p.yU.toFixed(1)}`;
    }
    for (let i = fPts.length - 1; i >= 0; i--) {
      bandDown += ` L${fPts[i].x.toFixed(1)} ${fPts[i].yL.toFixed(1)}`;
    }
    bandDown += ` L${startX.toFixed(1)} ${startY.toFixed(1)} Z`;
    const band = bandUp + bandDown;

    const fArea = `${fLine} L${(W_HIST + W_FORECAST).toFixed(1)} 130 L${startX.toFixed(1)} 130 Z`;

    return { line, area, fLine, fArea, band };
  });

  protected readonly statusEntries = computed(() =>
    Object.entries(this.statusBreakdown()).map(([status, count]) => ({ status, count })),
  );

  ngOnInit(): void {
    this.loadAll();
  }

  protected setPeriod(p: 'daily' | 'weekly' | 'monthly'): void {
    this.selectedPeriod.set(p);
    this.loadRevenue();
  }

  protected statusClass(status: string): string {
    return `status-${status.toLowerCase()}`;
  }

  private loadAll(): void {
    this.isLoading.set(true);
    this.loadKpi();
    this.loadRevenue();
    this.loadRecentOrders();
    this.loadStatusBreakdown();
    this.loadForecast();
  }

  private loadKpi(): void {
    this.http.get<KpiData>(`${API}/analytics/kpi`).subscribe({
      next: data => this.kpi.set(data),
      error: () => this.kpi.set({
        totalRevenue: 0, totalOrders: 0, avgOrderValue: 0, totalCustomers: 0,
      }),
    });
  }

  private loadRevenue(): void {
    this.http
      .get<RevenuePoint[]>(`${API}/analytics/revenue?period=${this.selectedPeriod()}`)
      .subscribe({
        next: data => {
          this.revenueData.set(data);
          this.isLoading.set(false);
        },
        error: () => this.isLoading.set(false),
      });
  }

  /**
   * Always fetch the 14-day daily ARIMA forecast. Even if the chart is showing
   * weekly/monthly historical data, the forecast still appends 14 daily points.
   */
  private loadForecast(): void {
    this.http.get<ForecastResponse>(`${API}/analytics/forecast?days=14`).subscribe({
      next: (res) => {
        if (res?.predictions) {
          this.forecastData.set(res.predictions);
          this.forecastModel.set(res.model ?? null);
        }
      },
      error: () => {
        // Forecast is optional — silently fail if the Python service is down
        this.forecastData.set([]);
      },
    });
  }

  private loadRecentOrders(): void {
    this.http.get<RecentOrder[]>(`${API}/analytics/recent-orders`).subscribe({
      next: data => this.recentOrders.set(data),
      error: () => {},
    });
  }

  private loadStatusBreakdown(): void {
    this.http
      .get<Record<string, number>>(`${API}/analytics/order-status-breakdown`)
      .subscribe({
        next: data => this.statusBreakdown.set(data),
        error: () => {},
      });
  }
}
