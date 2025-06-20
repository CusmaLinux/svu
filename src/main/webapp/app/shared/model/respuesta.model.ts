import { type IPqrs } from '@/shared/model/pqrs.model';
import { type IArchivoAdjunto } from '@/shared/model/archivo-adjunto.model';
import type { IUser } from './user.model';

export interface IRespuesta {
  id?: string;
  contenido?: string | null;
  fechaRespuesta?: Date | null;
  pqrs?: IPqrs | null;
  resolver?: IUser | null;
  byRequester?: boolean | null;
  archivosAdjuntosDTO?: IArchivoAdjunto[] | null;
  _transientAttachments?: IArchivoAdjunto[] | null;
}

export class Respuesta implements IRespuesta {
  constructor(
    public id?: string,
    public contenido?: string | null,
    public fechaRespuesta?: Date | null,
    public pqrs?: IPqrs | null,
    public resolver?: IUser | null,
    public byRequester?: boolean | null,
    public archivosAdjuntosDTO?: IArchivoAdjunto[] | null,
    public _transientAttachments?: IArchivoAdjunto[] | null,
  ) {}
}
