import {
  AfterViewInit, ChangeDetectionStrategy, Component, ElementRef,
  OnDestroy, ViewChild, inject, signal,
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

  protected readonly isLoading = signal(true);
  protected readonly hasError  = signal(false);

  private map: L.Map | null = null;

  ngAfterViewInit(): void {
    this.initMap();
    this.loadData();
  }

  ngOnDestroy(): void { this.map?.remove(); }

  private initMap(): void {
    this.map = L.map(this.mapContainer.nativeElement, {
      center: [45.9432, 24.9668],
      zoom: 7,
      zoomControl: false,
      attributionControl: false,
    });
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', { maxZoom: 19 })
      .addTo(this.map);
    L.control.zoom({ position: 'bottomleft' }).addTo(this.map);
  }

  private loadData(): void {
    this.http.get<MapPoint[]>(`${API}/analytics/map-data`).subscribe({
      next: (points) => { this.renderPins(points); this.isLoading.set(false); },
      error: () => { this.hasError.set(true); this.isLoading.set(false); },
    });
  }

  private renderPins(points: MapPoint[]): void {
    if (!this.map) return;

    for (const point of points) {
      // single neutral color for all pins — simpler than per-status
      const marker = L.circleMarker([point.lat, point.lng], {
        radius:      6,
        fillColor:   '#ededed',
        color:       '#ededed',
        weight:      0,
        opacity:     1,
        fillOpacity: 0.9,
      }).addTo(this.map!);

      marker.bindTooltip(
        `<div style="min-width:120px;">
          <div style="font-size:12px;color:#fff;margin-bottom:2px;">${point.destination}</div>
          <div style="font-size:11px;color:#999;">${point.count} orders</div>
        </div>`,
        { direction: 'top', offset: [0, -6], className: 'obsidian-tooltip' },
      );
    }
  }
}
