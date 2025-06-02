export const PROBLEM_BASE_URL = 'https://www.jhipster.tech/problem';
export const EMAIL_ALREADY_USED_TYPE = `${PROBLEM_BASE_URL}/email-already-used`;
export const LOGIN_ALREADY_USED_TYPE = `${PROBLEM_BASE_URL}/login-already-used`;

export enum PqrsStatus {
  Pending = 'PENDIENTE',
  InProcess = 'EN PROCESO',
  Resolved = 'RESUELTA',
  Closed = 'CERRADA',
  Rejected = 'RECHAZADA',
}

export enum NotificationType {
  PQRS_DUE_DATE_REMINDER = 'sse-notification.pqrs-due-date-reminder',
  PQRS_STATE_UPDATE = 'sse-notification.pqrs-state-update',
  PQRS_CREATED = 'sse-notification.pqrs-created',
}
