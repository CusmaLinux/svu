import type { IUser } from './user.model';

export interface INotification {
  id?: string;
  tipo: string;
  fecha?: Date;
  mensaje?: string;
  leido?: boolean | null;
  recipientDto?: IUser;
}

export class Notification implements INotification {
  constructor(
    public tipo: string,
    public id?: string,
    public fecha?: Date,
    public mensaje?: string,
    public leido?: boolean | null,
    public recipientDto?: IUser,
  ) {
    this.leido = this.leido ?? false;
  }
}
