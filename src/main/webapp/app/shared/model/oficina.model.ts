import { type IUser } from '@/shared/model/user.model';
import { type IPqrs } from './pqrs.model';

export interface IOficina {
  id?: string;
  nombre?: string;
  descripcion?: string | null;
  nivel?: string;
  oficinaSuperior?: string | null;
  responsableDTO?: IUser | null;
  pqrsList?: IPqrs[];
}

export class Oficina implements IOficina {
  constructor(
    public id?: string,
    public nombre?: string,
    public descripcion?: string | null,
    public nivel?: string,
    public oficinaSuperior?: string | null,
    public responsableDTO?: IUser | null,
    public pqrsList: IPqrs[] = [],
  ) {}
}
