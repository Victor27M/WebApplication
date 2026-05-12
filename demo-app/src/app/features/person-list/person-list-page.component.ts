import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { ConfirmDeleteDialogComponent } from '../../components/confirm-delete-dialog/confirm-delete-dialog.component';
import {
  PersonFormDialogComponent,
  PersonFormDialogData,
  PersonFormDialogResult,
} from '../../components/person-form-dialog/person-form-dialog.component';
import { CreatePersonDto, Person, UpdatePersonDto } from '../../models/person.model';
import { PersonListStore } from './person-list.store';

@Component({
  selector: 'app-person-list-page',
  imports: [MatTableModule, MatButtonModule, MatIconModule, MatDialogModule],
  templateUrl: './person-list-page.component.html',
  styleUrl: './person-list-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PersonListPageComponent {
  private readonly dialog     = inject(MatDialog);
  private readonly store      = inject(PersonListStore);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly persons          = this.store.persons;
  protected readonly hasError         = this.store.hasError;
  protected readonly isLoading        = this.store.isLoading;
  protected readonly displayedColumns = ['name', 'age', 'email', 'role', 'actions'];

  constructor() { this.store.load(); }

  protected openCreateDialog(): void {
    if (this.isLoading()) return;
    this.dialog
      .open<PersonFormDialogComponent, PersonFormDialogData, PersonFormDialogResult>(
        PersonFormDialogComponent,
        { data: { title: 'Create Person', submitLabel: 'Create', showPasswordField: true } },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (!result) return;
        const dto: CreatePersonDto = {
          name: result.name, email: result.email,
          password: result.password ?? '', age: result.age, role: result.role,
        };
        this.store.create(dto);
      });
  }

  protected openEditDialog(person: Person): void {
    if (this.isLoading()) return;
    this.dialog
      .open<PersonFormDialogComponent, PersonFormDialogData, PersonFormDialogResult>(
        PersonFormDialogComponent,
        {
          data: {
            title: 'Edit Person', submitLabel: 'Save', showPasswordField: false,
            initialValue: person,
          },
        },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (!result) return;
        const dto: UpdatePersonDto = {
          name: result.name, email: result.email, age: result.age,
          role: result.role, password: person.password, // kept from existing, store overwrites anyway
        };
        this.store.update(person.id, dto);   // ← two separate args
      });
  }

  protected openDeleteDialog(person: Person): void {
    if (this.isLoading()) return;
    this.dialog
      .open(ConfirmDeleteDialogComponent, { data: { name: person.name } })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((confirmed) => {
        if (confirmed) this.store.remove(person.id);   // ← remove not delete
      });
  }
}
