export enum PageSize {
  DEFAULT = 15,
  LARGE = 200,
  BULK = 2000,
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: SliceResponse;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface SliceResponse {
  size: number;
  number: number;
  totalElements: number;
  totalPages: number;
}
