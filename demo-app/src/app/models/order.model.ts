export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED';

export interface OrderItem {
  id: string;
  product: {
    id: string;
    name: string;
    price: number;
  };
  quantity: number;
}

export interface Order {
  id: string;
  person: {
    id: string;
    name: string;
    email: string;
  };
  items: OrderItem[];
  destination: string;
  orderDate: string;
  status: OrderStatus;
}

export interface OrderItemDto {
  productId: string;
  quantity: number;
}

export interface CreateOrderDto {
  personId: string;
  items: OrderItemDto[];
  destination: string;
  status?: OrderStatus;
}

export interface UpdateOrderDto {
  personId: string;
  items: OrderItemDto[];
  destination: string;
  status?: OrderStatus;
}
