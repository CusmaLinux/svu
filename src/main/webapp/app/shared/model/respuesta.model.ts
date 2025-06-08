import { type IPqrs } from '@/shared/model/pqrs.model';
import { type IArchivoAdjunto } from '@/shared/model/archivo-adjunto.model';

export interface IRespuesta {
  id?: string;
  contenido?: string;
  fechaRespuesta?: Date;
  pqr?: IPqrs | null;
  archivosAdjuntosDTO?: IArchivoAdjunto[] | null;
  _transientAttachments?: IArchivoAdjunto[];
}

export class Respuesta implements IRespuesta {
  constructor(
    public id?: string,
    public contenido?: string,
    public fechaRespuesta?: Date,
    public pqr?: IPqrs | null,
    public archivosAdjuntosDTO?: IArchivoAdjunto[] | null,
    public _transientAttachments?: IArchivoAdjunto[],
  ) {}
}
