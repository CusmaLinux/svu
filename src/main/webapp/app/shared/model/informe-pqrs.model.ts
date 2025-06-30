import { type IOficina } from '@/shared/model/oficina.model';

export interface IInformePqrs {
  id?: string;
  fechaInicio?: string | number | Date;
  fechaFin?: string | number | Date;
  totalPqrs?: number;
  totalResueltas?: number;
  totalPendientes?: number;
  oficina?: IOficina | null;
}

export class InformePqrs implements IInformePqrs {
  constructor(
    public id?: string,
    public fechaInicio?: string | number | Date,
    public fechaFin?: string | number | Date,
    public totalPqrs?: number,
    public totalResueltas?: number,
    public totalPendientes?: number,
    public oficina?: IOficina | null,
  ) {}
}
