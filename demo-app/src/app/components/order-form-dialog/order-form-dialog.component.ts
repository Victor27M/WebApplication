import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
} from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { Person } from '../../models/person.model';
import { Product } from '../../models/product.model';
import { OrderItemDto, OrderStatus } from '../../models/order.model';

export interface OrderFormDialogData {
  title: string;
  submitLabel?: string;
  showStatusField?: boolean;   // ← add this
  persons: Person[];
  products: Product[];
  initialValue?: OrderFormInitialValue | null;
}

export interface OrderFormInitialValue {
  personId: string;
  items: OrderItemDto[];
  destination: string;
  status: OrderStatus;
}

export type OrderFormValue = OrderFormInitialValue;
export type OrderFormDialogResult = OrderFormValue | undefined;

@Component({
  selector: 'app-order-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './order-form-dialog.component.html',
  styleUrl: './order-form-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderFormDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<OrderFormDialogComponent>);
  protected readonly data = inject<OrderFormDialogData>(MAT_DIALOG_DATA);

  protected readonly statuses: OrderStatus[] = [
    'PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED',
  ];

  protected readonly form = this.fb.nonNullable.group({
    personId: ['', [Validators.required]],
    destination: ['', [Validators.required, Validators.minLength(3)]],
    status: ['PENDING' as OrderStatus, [Validators.required]],
    items: this.fb.array([]),
  });

  get itemsArray(): FormArray {
    return this.form.get('items') as FormArray;
  }

  ngOnInit(): void {
    if (this.data.initialValue) {
      this.form.patchValue({
        personId: this.data.initialValue.personId,
        destination: this.data.initialValue.destination,
        status: this.data.initialValue.status,
      });
      this.data.initialValue.items.forEach((item) => this.addItem(item));
    } else {
      this.addItem();
    }
  }

  protected addItem(item?: OrderItemDto): void {
    this.itemsArray.push(
      this.fb.nonNullable.group({
        productId: [item?.productId ?? '', [Validators.required]],
        quantity: [item?.quantity ?? 1, [Validators.required, Validators.min(1)]],
      }),
    );
  }

  protected removeItem(index: number): void {
    if (this.itemsArray.length > 1) {
      this.itemsArray.removeAt(index);
    }
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.dialogRef.close(this.form.getRawValue() as OrderFormValue);
  }

  protected cancel(): void {
    this.dialogRef.close(undefined);
  }
}
