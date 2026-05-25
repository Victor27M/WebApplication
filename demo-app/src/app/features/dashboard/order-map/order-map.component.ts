import {
  AfterViewInit, ChangeDetectionStrategy, Component, ElementRef,
  OnDestroy, ViewChild, inject, signal, NgZone,
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

@Component({
  selector: 'app-order-map',
  templateUrl: './order-map.component.html',
  styleUrl: './order-map.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderMapComponent implements AfterViewInit, OnDestroy {
  @ViewChild('mapContainer') mapContainer!: ElementRef<HTMLDivElement>;

  private readonly http = inject(HttpClient);
  private readonly zone = inject(NgZone);

  protected readonly isLoading = signal(true);
  protected readonly hasError  = signal(false);
  protected readonly pinCount  = signal(0);

  private map: L.Map | null = null;
  private pinOverlay?: HTMLDivElement;
  private points: MapPoint[] = [];

  ngAfterViewInit(): void {
    this.zone.runOutsideAngular(() => {
      // Container has a fixed size from CSS, so Leaflet initializes correctly first time
      setTimeout(() => {
        this.initMap();
        this.loadData();
      }, 100);
    });
  }

  ngOnDestroy(): void {
    this.map?.remove();
  }

  private initMap(): void {
    const container = this.mapContainer.nativeElement;

    this.map = L.map(container, {
      dragging: false,
      scrollWheelZoom: false,
      doubleClickZoom: false,
      boxZoom: false,
      keyboard: false,
      touchZoom: false,
      zoomControl: false,
      attributionControl: false,
    }).setView([45.94, 25.0], 7);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      subdomains: ['a', 'b', 'c'],
    }).addTo(this.map);
  }

  private loadData(): void {
    this.http.get<MapPoint[]>(`${API}/analytics/map-data`).subscribe({
      next: (points) => {
        this.points = points.filter(p => p.lat != null && p.lng != null);
        this.setupOverlay();
        this.renderPins();
        // One more paint after tiles have a moment to settle
        setTimeout(() => this.renderPins(), 400);
        this.zone.run(() => {
          this.isLoading.set(false);
          this.pinCount.set(this.points.length);
        });
      },
      error: () => {
        this.zone.run(() => {
          this.hasError.set(true);
          this.isLoading.set(false);
        });
      },
    });
  }

  private setupOverlay(): void {
    if (!this.map || this.pinOverlay) return;
    const container = this.mapContainer.nativeElement;
    this.pinOverlay = document.createElement('div');
    this.pinOverlay.style.cssText = `
      position: absolute; top: 0; left: 0;
      width: 100%; height: 100%;
      pointer-events: none; z-index: 1000;
    `;
    container.appendChild(this.pinOverlay);
  }

  private renderPins(): void {
    if (!this.map || !this.pinOverlay) return;
    this.pinOverlay.innerHTML = '';

    for (const point of this.points) {
      const pixel = this.map.latLngToContainerPoint([point.lat, point.lng]);

      const halo = document.createElement('div');
      halo.style.cssText = `
        position: absolute;
        left: ${pixel.x}px; top: ${pixel.y}px;
        width: 26px; height: 26px;
        margin-left: -13px; margin-top: -13px;
        background: #f59e0b; border-radius: 50%; opacity: 0.22;
      `;

      const dot = document.createElement('div');
      dot.title = `${point.destination} • ${point.count} orders`;
      dot.style.cssText = `
        position: absolute;
        left: ${pixel.x}px; top: ${pixel.y}px;
        width: 14px; height: 14px;
        margin-left: -7px; margin-top: -7px;
        background: #f59e0b;
        border: 2px solid #ffffff;
        border-radius: 50%;
        box-shadow: 0 0 10px rgba(245, 158, 11, 0.7), 0 0 0 1px rgba(0,0,0,0.6);
        pointer-events: auto; cursor: pointer;
        transition: transform 0.15s;
      `;
      dot.addEventListener('mouseenter', () => { dot.style.transform = 'scale(1.3)'; });
      dot.addEventListener('mouseleave', () => { dot.style.transform = 'scale(1)'; });

      const label = document.createElement('div');
      label.style.cssText = `
        position: absolute;
        left: ${pixel.x + 11}px; top: ${pixel.y - 8}px;
        padding: 1px 6px;
        background: rgba(10,10,10,0.85);
        border-radius: 3px;
        font-size: 10px; font-weight: 600; color: #ffffff;
        white-space: nowrap;
        font-family: 'Inter', 'Roboto', sans-serif;
        pointer-events: none;
      `;
      label.textContent = `${point.destination} · ${point.count}`;

      this.pinOverlay.appendChild(halo);
      this.pinOverlay.appendChild(dot);
      this.pinOverlay.appendChild(label);
    }
  }
}
