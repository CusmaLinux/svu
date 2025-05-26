import { type IPqrs } from '@/shared/model/pqrs.model';

export interface IRespuesta {
  id?: string;
  contenido?: string;
  fechaRespuesta?: Date;
  pqr?: IPqrs | null;
}

export class Respuesta implements IRespuesta {
  constructor(
    public id?: string,
    public contenido?: string,
    public fechaRespuesta?: Date,
    public pqr?: IPqrs | null,
  ) {}
}
