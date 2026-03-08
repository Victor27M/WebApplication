export interface Person {
  id: string;
  name: string;
  age: number;
  email: string;
  password: string;
  role: string;
}

export type CreatePersonDto = Omit<Person, 'id'>;
export type UpdatePersonDto = Omit<Person, 'id'>;
