import { type IOficina } from '@/shared/model/oficina.model';
import { type IArchivoAdjunto } from '@/shared/model/archivo-adjunto.model';
import type { IRespuesta } from './respuesta.model';

export interface IPqrs {
  id?: string;
  fileNumber?: string;
  type?: string;
  titulo?: string;
  descripcion?: string;
  requesterEmail?: string | null;
  accessToken?: string;
  daysToReply?: string;
  fechaCreacion?: Date | string | number;
  fechaLimiteRespuesta?: Date | string | number;
  estado?: string;
  oficinaResponder?: IOficina | null;
  archivosAdjuntosDTO?: IArchivoAdjunto[] | null;
  _transientAttachments?: IArchivoAdjunto[];
  _transientResponses?: IRespuesta[];
}

export class Pqrs implements IPqrs {
  constructor(
    public id?: string,
    public fileNumber?: string,
    public type?: string,
    public titulo?: string,
    public descripcion?: string,
    public requesterEmail?: string | null,
    public accessToken?: string,
    public daysToReply?: string,
    public fechaCreacion?: Date | string | number,
    public fechaLimiteRespuesta?: Date | string | number,
    public estado?: string,
    public oficinaResponder?: IOficina | null,
    public archivosAdjuntosDTO?: IArchivoAdjunto[] | null,
    public _transientAttachments?: IArchivoAdjunto[],
    public _transientResponses?: IRespuesta[],
  ) {}
}
