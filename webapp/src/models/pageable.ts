export enum SortDirection {
    ASC = "ASC",
    DESC = "DESC"
}

export interface SortOrder {
    direction: SortDirection,
    property: string
}

export interface Sort {
    orders: SortOrder[]
}

export interface Pageable {
    page: number,
    pageSize: number,
    sort?: Sort,
}