export interface PageResponse<T> {
  records: T[];
  pageNo: number;
  pageSize: number;
  total: number;
}
