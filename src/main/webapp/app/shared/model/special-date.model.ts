export interface ISpecialDate {
  id?: string;
  date?: string | null;
  description?: string;
}

export class SpecialDate implements ISpecialDate {
  constructor(
    public id?: string,
    public date?: string | null,
    public description?: string,
  ) {}
}
