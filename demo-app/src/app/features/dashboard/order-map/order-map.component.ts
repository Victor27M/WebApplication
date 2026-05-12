import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import * as L from 'leaflet';

const API = 'http://localhost:8080';

interface MapPoint {
  lat: number;
  lng: number;
  destination: string;
  count: number;
  statuses: Record<string, number>;
}

const STATUS_COLOURS: Record<string, string> = {
  DELIVERED: '#16a34a',
  SHIPPED:   '#a78bfa',
  CONFIRMED: '#60a5fa',
  PENDING:   '#f59e0b',
  CANCELLED: '#f87171',
};

@Component({
  selector: 'app-order-map',
  templateUrl: './order-map.component.html',
  styleUrl: './order-map.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderMapComponent implements AfterViewInit, OnDestroy {
  @ViewChild('mapContainer') mapContainer!: ElementRef<HTMLDivElement>;

  private readonly http = inject(HttpClient);

  protected readonly isLoading = signal(true);
  protected readonly hasError  = signal(false);

  private map: L.Map | null = null;

  ngAfterViewInit(): void {
    this.initMap();
    this.loadData();
  }

  ngOnDestroy(): void {
    this.map?.remove();
  }

  private initMap(): void {
    this.map = L.map(this.mapContainer.nativeElement, {
      center: [45.9432, 24.9668],
      zoom: 7,
      zoomControl: false,
      attributionControl: false,
    });

    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      maxZoom: 19,
    }).addTo(this.map);

    L.control.zoom({ position: 'bottomleft' }).addTo(this.map);
  }

  private loadData(): void {
    this.http.get<MapPoint[]>(`${API}/analytics/map-data`).subscribe({
      next: (points) => {
        this.renderPins(points);
        this.isLoading.set(false);
      },
      error: () => {
        this.hasError.set(true);
        this.isLoading.set(false);
      },
    });
  }

  private renderPins(points: MapPoint[]): void {
    if (!this.map) return;

    for (const point of points) {
      const dominant = this.dominantStatus(point.statuses);
      const colour   = STATUS_COLOURS[dominant] ?? '#888';

      // Fixed 8px radius — circleMarker is always in screen pixels
      // so it stays the same size regardless of zoom level
      const marker = L.circleMarker([point.lat, point.lng], {
        radius:      8,
        fillColor:   colour,
        color:       'rgba(255,255,255,0.5)',
        weight:      1.5,
        opacity:     1,
        fillOpacity: 0.85,
      }).addTo(this.map!);

      const rows = Object.entries(point.statuses)
        .sort((a, b) => b[1] - a[1])
        .map(([s, c]) =>
          `<div style="display:flex;align-items:center;gap:6px;font-size:11px;color:#aaa;">
            <span style="width:6px;height:6px;border-radius:50%;background:${STATUS_COLOURS[s] ?? '#888'};flex-shrink:0;"></span>
            <span>${s}</span>
            <span style="margin-left:auto;color:#fff;font-weight:500;">${c}</span>
          </div>`)
        .join('');

      marker.bindTooltip(
        `<div style="min-width:140px;display:flex;flex-direction:column;gap:4px;">
          <span style="font-size:13px;font-weight:500;color:#fff;margin-bottom:2px;">${point.destination}</span>
          <span style="font-size:11px;color:#888;margin-bottom:4px;">${point.count} orders</span>
          ${rows}
        </div>`,
        { direction: 'top', offset: [0, -10], className: 'obsidian-tooltip' },
      );
    }
  }

  private dominantStatus(statuses: Record<string, number>): string {
    return Object.entries(statuses).sort((a, b) => b[1] - a[1])[0]?.[0] ?? 'PENDING';
  }
}
