export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  stock: number;
}

export type CreateProductDto = Omit<Product, 'id'>;
export type UpdateProductDto = Omit<Product, 'id'>;
