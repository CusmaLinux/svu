import axios, { AxiosError, type AxiosResponse } from 'axios';
import type { ReportNarrative } from './report-narrative';
import type { NarrativePreview, PqrsStatistics, PreviewFilter, StatisticsConfiguration } from './statistics.model';

const base = 'api/reports/gestion-pqrsd';
const pageSize = 100;
const xlsx = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
const docx = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
export type OfficeOption = { id: string; nombre: string };
export interface SavedReportSummary {
  id: string;
  title: string;
  filter: { start: string; end: string; scope: 'INSTITUTIONAL' | 'OFFICE'; officeId: string | null };
  office: { idType: string; id: string; name: string | null } | null;
  cutoff: string;
  createdAt: string;
  modifiedAt: string;
  generatedAt: string;
  version: number;
  received: number;
}
export interface SavedReport extends SavedReportSummary {
  statistics: PqrsStatistics;
  narrative: ReportNarrative;
  createdBy: string;
  modifiedBy: string;
  modelVersion: number;
  methodologyVersion: number;
  catalogVersion: number;
}
export interface SavedReportPage {
  reports: SavedReportSummary[];
  total: number;
  page: number;
  size: number;
}

// The shared interceptor can resolve without a response after handling an expired session.
function responseOrExpired<T>(response: AxiosResponse<T> | undefined): AxiosResponse<T> {
  if (response) return response;
  throw Object.assign(new AxiosError('Session expired'), { response: { status: 401 } });
}

function fileSuffix(filter: PreviewFilter) {
  const scope = filter.scope === 'OFFICE' ? 'Dependencia' : 'Institucional';
  return `${scope}_${filter.start}_${filter.end}`;
}

function downloadFile(response: AxiosResponse<Blob>, mime: string, suffix: string) {
  const contentType = String(response.headers?.['content-type'] ?? '')
    .split(';')[0]
    .trim()
    .toLowerCase();
  if (contentType !== mime || !(response.data instanceof Blob) || response.data.size === 0) {
    throw new Error('Invalid download response');
  }
  return { blob: response.data, filename: 'Informe_PQRSD_' + suffix };
}

export const statisticsService = {
  async configuration(signal: AbortSignal) {
    return responseOrExpired(await axios.get<StatisticsConfiguration>(base + '/configuration', { signal })).data;
  },
  async preview(filter: PreviewFilter, signal: AbortSignal) {
    return responseOrExpired(await axios.get<PqrsStatistics>(base + '/preview', { params: filter, signal })).data;
  },
  async narrativePreview(filter: PreviewFilter, signal: AbortSignal) {
    return responseOrExpired(await axios.post<NarrativePreview>(base + '/narrative-previews', { filter }, { signal })).data;
  },
  async exportWord(receipt: string, filter: PreviewFilter, narrative: ReportNarrative, signal: AbortSignal) {
    const response = responseOrExpired(
      await axios.post<Blob>(base + '/export/docx', { receipt, filter, narrative }, { signal, responseType: 'blob' }),
    );
    return downloadFile(response, docx, `Preliminar_${fileSuffix(filter)}.docx`);
  },
  async exportXlsx(filter: PreviewFilter, signal: AbortSignal) {
    const response = responseOrExpired(await axios.get<Blob>(base + '/export/xlsx', { params: filter, signal, responseType: 'blob' }));
    return downloadFile(response, xlsx, `${fileSuffix(filter)}.xlsx`);
  },
  async save(receipt: string, filter: PreviewFilter, narrative: ReportNarrative) {
    return responseOrExpired(await axios.post<SavedReport>(base + '/saved', { receipt, filter, narrative })).data;
  },
  async savedReports(params: { page: number; size: number; scope?: string; start?: string; end?: string }) {
    return responseOrExpired(await axios.get<SavedReportPage>(base + '/saved', { params })).data;
  },
  async savedReport(id: string) {
    return responseOrExpired(await axios.get<SavedReport>(base + '/saved/' + encodeURIComponent(id))).data;
  },
  async updateSavedNarrative(id: string, expectedVersion: number, narrative: ReportNarrative) {
    return responseOrExpired(
      await axios.put<SavedReport>(base + '/saved/' + encodeURIComponent(id) + '/narrative', { expectedVersion, narrative }),
    ).data;
  },
  async exportSavedWord(id: string) {
    const response = responseOrExpired(
      await axios.get<Blob>(base + '/saved/' + encodeURIComponent(id) + '/export/docx', { responseType: 'blob' }),
    );
    return downloadFile(response, docx, `Guardado_${id}.docx`);
  },
  async exportSavedXlsx(id: string) {
    const response = responseOrExpired(
      await axios.get<Blob>(base + '/saved/' + encodeURIComponent(id) + '/export/xlsx', { responseType: 'blob' }),
    );
    return downloadFile(response, xlsx, `Guardado_${id}.xlsx`);
  },
  async offices(signal: AbortSignal) {
    const offices = new Map<string, OfficeOption>();
    let read = 0;
    for (let page = 0; !signal.aborted; page++) {
      const { data, headers } = responseOrExpired(
        await axios.get<OfficeOption[]>('api/oficinas/catalogo', {
          params: { page, size: pageSize, sort: ['nombre,asc', 'id,asc'] },
          paramsSerializer: { indexes: null },
          signal,
        }),
      );
      for (const office of data) offices.set(office.id, office);
      read += data.length;
      const total = Number(headers['x-total-count']);
      if (data.length === 0 || (Number.isFinite(total) ? read >= total : data.length < pageSize)) break;
    }
    return [...offices.values()];
  },
};
