import type { PreviewFilter } from './statistics.model';

export const narrativeSections = {
  1: [
    { id: 'introduction', label: 'Introducción' },
    { id: 'objective', label: 'Objetivo' },
    { id: 'legalFramework', label: 'Marco normativo' },
  ],
  2: [{ id: 'requestsAnalysis', label: 'Análisis del comportamiento de las solicitudes' }],
  3: [{ id: 'usersChannelsAnalysis', label: 'Observaciones y análisis de usuarios y canales' }],
  4: [{ id: 'routingAnalysis', label: 'Observaciones y análisis de direccionamiento' }],
  5: [{ id: 'officesAnalysis', label: 'Análisis de resultados por dependencia' }],
  6: [{ id: 'indicatorsInterpretation', label: 'Interpretación de resultados y limitaciones' }],
  7: [
    { id: 'conclusions', label: 'Conclusiones' },
    { id: 'recommendations', label: 'Recomendaciones' },
  ],
} as const;

export const narrativeLimit = 4000;
export type NarrativeSection = keyof typeof narrativeSections;
export type NarrativeField = (typeof narrativeSections)[NarrativeSection][number]['id'];
export type ReportNarrative = Partial<Record<NarrativeField, string>>;
export type NarrativeSlot = (props: { section: NarrativeSection }) => unknown;

export interface NarrativeDraft {
  readonly previewId: number;
  readonly filter: Readonly<PreviewFilter>;
  readonly fields: ReportNarrative;
}
