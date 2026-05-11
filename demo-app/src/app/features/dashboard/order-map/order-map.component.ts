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

// Status color map — matches the obsidian status badge palette
const STATUS_COLOURS: Record<string, string> = {
  DELIVERED: '#16a34a',
  SHIPPED:   '#a78bfa',
  CONFIRMED: '#60a5fa',
  PENDING:   '#f59e0b',
  CANCELLED: '#f87171',
};

@Component({
  selector: 'app-order-map',
  standalone: true,
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
      center: [45.9432, 24.9668], // centred on Romania
      zoom: 7,
      zoomControl: false,
      attributionControl: false,
    });

    // Dark map tiles to match the obsidian theme
    L.tileLayer(
      'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
      { maxZoom: 19 },
    ).addTo(this.map);

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
      const dominantStatus = this.dominantStatus(point.statuses);
      const colour = STATUS_COLOURS[dominantStatus] ?? '#888';

      const icon = L.divIcon({
        className: 'order-pin',
        html: `
          <div class="pin-ring" style="background:${colour}"></div>
          <div class="pin-dot"  style="background:${colour}"></div>
        `,
        iconSize: [18, 18],
        iconAnchor: [9, 9],
      });

      const marker = L.marker([point.lat, point.lng], { icon }).addTo(this.map);

      const tooltipHtml = `
        <div class="pin-tooltip">
          <div class="pin-tip-title">${point.destination}</div>
          <div class="pin-tip-count">${point.count} order${point.count !== 1 ? 's' : ''}</div>
          ${Object.entries(point.statuses)
        .map(([status, count]) => `
              <div class="pin-tip-row">
                <span class="pin-tip-dot" style="background:${STATUS_COLOURS[status] ?? '#888'}"></span>
                <span>${status}</span>
                <span class="pin-tip-num">${count}</span>
              </div>
            `).join('')}
        </div>
      `;

      marker.bindTooltip(tooltipHtml, {
        direction: 'top',
        offset: [0, -10],
        className: 'obsidian-tooltip',
      });
    }
  }

  private dominantStatus(statuses: Record<string, number>): string {
    let max = '';
    let maxVal = -1;
    for (const [status, count] of Object.entries(statuses)) {
      if (count > maxVal) {
        maxVal = count;
        max = status;
      }
    }
    return max;
  }
}
