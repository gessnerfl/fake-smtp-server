export interface Page<T> {
    number: number
    numberOfElements: number
    size:  number,
    totalPages: number,
    totalElements: number,
    content: T[]
}