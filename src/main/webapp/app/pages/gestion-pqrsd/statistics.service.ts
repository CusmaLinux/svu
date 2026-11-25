import axios from 'axios';
import type { PqrsStatistics, PreviewFilter, StatisticsConfiguration } from './statistics.model';

const base = 'api/reports/gestion-pqrsd';
const pageSize = 100;
export type OfficeOption = { id: string; nombre: string };

export const statisticsService = {
  async configuration(signal: AbortSignal) {
    return (await axios.get<StatisticsConfiguration>(base + '/configuration', { signal })).data;
  },
  async preview(filter: PreviewFilter, signal: AbortSignal) {
    return (await axios.get<PqrsStatistics>(base + '/preview', { params: filter, signal })).data;
  },
  async offices(signal: AbortSignal) {
    const offices = new Map<string, OfficeOption>();
    let read = 0;
    for (let page = 0; !signal.aborted; page++) {
      const { data, headers } = await axios.get<OfficeOption[]>('api/oficinas/catalogo', {
        params: { page, size: pageSize, sort: ['nombre,asc', 'id,asc'] },
        paramsSerializer: { indexes: null },
        signal,
      });
      for (const office of data) offices.set(office.id, office);
      read += data.length;
      const total = Number(headers['x-total-count']);
      if (data.length === 0 || (Number.isFinite(total) ? read >= total : data.length < pageSize)) break;
    }
    return [...offices.values()];
  },
};
