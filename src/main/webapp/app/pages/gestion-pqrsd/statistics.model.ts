import type { ComposeOption } from 'echarts/core';
import type { BarSeriesOption, LineSeriesOption } from 'echarts/charts';
import type { DataZoomComponentOption, GridComponentOption, LegendComponentOption, TooltipComponentOption } from 'echarts/components';

export type ChartOptions = ComposeOption<
  BarSeriesOption | LineSeriesOption | GridComponentOption | LegendComponentOption | TooltipComponentOption | DataZoomComponentOption
>;

export type Scope = 'INSTITUTIONAL' | 'OFFICE';
export type PreviewFilter = { start: string; end: string } & ({ scope: 'INSTITUTIONAL' } | { scope: 'OFFICE'; officeId: string });
export type Availability = 'PARTIAL_SOURCE' | 'SOURCE_UNAVAILABLE' | 'DEFINITION_PENDING';

export interface StatisticsConfiguration {
  scopes: Scope[];
  zone: string;
  maxPeriodDays: number;
  maxOfficeIdLength: number;
  availableIndicators: string[];
  deferredIndicators: Record<string, Availability>;
  limitations: string[];
}

export interface OfficeCount {
  category: 'ASSIGNED' | 'UNASSIGNED' | 'UNVERIFIABLE';
  idType: string | null;
  id: string | null;
  name: string | null;
  count: number;
}

export interface MonthlyStatistics {
  month: string;
  received: number;
  complete: boolean;
  previousCount: number | null;
  comparison: 'CALCULATED' | 'PARTIAL_MONTH' | 'NO_COMPARABLE_BASE' | 'NO_CALCULABLE';
  variationPercent: number | null;
  types: Record<string, number>;
  currentOffices: OfficeCount[];
}

export interface PqrsStatistics {
  filter: { start: string; end: string; scope: Scope; officeId: string | null };
  period: { zone: string; startInclusive: string; endExclusive: string; effectiveCutoff: string; observedAt: string };
  coverage: { status: string; warnings: string[]; limitations: string[] };
  received: number;
  months: MonthlyStatistics[];
  types: { code: string; count: number; denominator: number; percent: number | null }[];
  currentStates: Record<string, number>;
  currentOffices: OfficeCount[];
  deferredIndicators: Record<string, Availability>;
}

export interface NarrativePreview {
  receipt: string;
  expiresAt: string;
  modelVersion: number;
  methodologyVersion: number;
  catalogVersion: number;
  resolvedOffice: { idType: string; id: string; name: string | null } | null;
  availableIndicators: string[];
  statistics: PqrsStatistics;
}
