import { type IOficina } from '@/shared/model/oficina.model';
import { type IArchivoAdjunto } from '@/shared/model/archivo-adjunto.model';

export interface IPqrs {
  id?: string;
  titulo?: string;
  descripcion?: string;
  fechaCreacion?: Date | undefined | null;
  fechaLimiteRespuesta?: Date | undefined | null;
  estado?: string;
  oficinaResponder?: IOficina | null;
  archivosAdjuntosDTO?: IArchivoAdjunto[] | null;
}

export class Pqrs implements IPqrs {
  constructor(
    public id?: string,
    public titulo?: string,
    public descripcion?: string,
    public fechaCreacion?: Date | undefined,
    public fechaLimiteRespuesta?: Date | null,
    public estado?: string,
    public oficinaResponder?: IOficina | null,
    public archivosAdjuntosDTO?: IArchivoAdjunto[] | null,
  ) {}
}
