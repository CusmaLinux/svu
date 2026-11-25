import type { PqrsStatistics, StatisticsConfiguration } from './statistics.model';

export const testConfiguration: StatisticsConfiguration = {
  scopes: ['INSTITUTIONAL', 'OFFICE'],
  zone: 'America/Bogota',
  maxPeriodDays: 366,
  maxOfficeIdLength: 128,
  availableIndicators: ['IND-01', 'IND-02', 'IND-03', 'IND-04', 'IND-05', 'IND-06', 'IND-07'],
  deferredIndicators: {
    'IND-08': 'PARTIAL_SOURCE',
    'IND-09': 'SOURCE_UNAVAILABLE',
    'IND-10': 'PARTIAL_SOURCE',
    'IND-11': 'SOURCE_UNAVAILABLE',
    'IND-12': 'SOURCE_UNAVAILABLE',
    'IND-13': 'PARTIAL_SOURCE',
    'IND-14': 'DEFINITION_PENDING',
    'IND-15': 'DEFINITION_PENDING',
    'IND-16': 'SOURCE_UNAVAILABLE',
    'IND-17': 'SOURCE_UNAVAILABLE',
    'IND-18': 'SOURCE_UNAVAILABLE',
    'IND-19': 'SOURCE_UNAVAILABLE',
  },
  limitations: [
    'ONLY_DATED_ROOTS',
    'OFFICE_AND_STATE_AT_OBSERVATION',
    'HISTORICAL_COMPLETENESS_UNMEASURED',
    'OBSERVATION_NOT_TRANSACTIONAL',
  ],
};

export const testReport: PqrsStatistics = {
  filter: { start: '2024-01-01', end: '2024-03-20', scope: 'INSTITUTIONAL', officeId: null },
  period: {
    zone: testConfiguration.zone,
    startInclusive: '2024-01-01T05:00:00Z',
    endExclusive: '2024-03-21T05:00:00Z',
    effectiveCutoff: '2024-03-21T05:00:00Z',
    observedAt: '2024-03-22T13:15:00Z',
  },
  coverage: {
    status: 'PARTIAL',
    warnings: ['TYPE_HISTORICALLY_AMBIGUOUS', 'OFFICE_UNASSIGNED', 'OFFICE_UNVERIFIABLE'],
    limitations: testConfiguration.limitations,
  },
  received: 13,
  currentStates: { RESOLVED: 6, PENDING: 3, IN_PROGRESS: 2, CLOSED: 1, REJECTED: 0, UNKNOWN: 1 },
  currentOffices: [
    { category: 'ASSIGNED', idType: 'string', id: 'synthetic-a', name: 'Atención al Usuario', count: 8 },
    { category: 'ASSIGNED', idType: 'string', id: 'synthetic-b', name: 'Registro Académico', count: 3 },
    { category: 'UNASSIGNED', idType: null, id: null, name: null, count: 1 },
    { category: 'UNVERIFIABLE', idType: null, id: null, name: null, count: 1 },
  ],
  types: [
    { code: 'REQUEST', count: 6, denominator: 13, percent: 46.2 },
    { code: 'COMPLAINT', count: 3, denominator: 13, percent: 23.1 },
    { code: 'DEMAND', count: 1, denominator: 13, percent: 7.7 },
    { code: 'HISTORICALLY_AMBIGUOUS', count: 2, denominator: 13, percent: 15.4 },
    { code: 'UNKNOWN', count: 1, denominator: 13, percent: 7.7 },
  ],
  months: [
    {
      month: '2024-01',
      received: 6,
      complete: true,
      previousCount: 0,
      comparison: 'NO_CALCULABLE',
      variationPercent: null,
      types: { REQUEST: 3, COMPLAINT: 2, DEMAND: 1 },
      currentOffices: [
        { category: 'ASSIGNED', idType: 'string', id: 'synthetic-a', name: 'Atención al Usuario', count: 4 },
        { category: 'ASSIGNED', idType: 'string', id: 'synthetic-b', name: 'Registro Académico', count: 2 },
      ],
    },
    {
      month: '2024-02',
      received: 3,
      complete: true,
      previousCount: 6,
      comparison: 'CALCULATED',
      variationPercent: -50,
      types: { REQUEST: 2, COMPLAINT: 1 },
      currentOffices: [
        { category: 'ASSIGNED', idType: 'string', id: 'synthetic-a', name: 'Atención al Usuario', count: 2 },
        { category: 'UNASSIGNED', idType: null, id: null, name: null, count: 1 },
      ],
    },
    {
      month: '2024-03',
      received: 4,
      complete: false,
      previousCount: 3,
      comparison: 'PARTIAL_MONTH',
      variationPercent: null,
      types: { REQUEST: 1, HISTORICALLY_AMBIGUOUS: 2, UNKNOWN: 1 },
      currentOffices: [
        { category: 'ASSIGNED', idType: 'string', id: 'synthetic-a', name: 'Atención al Usuario', count: 2 },
        { category: 'ASSIGNED', idType: 'string', id: 'synthetic-b', name: 'Registro Académico', count: 1 },
        { category: 'UNVERIFIABLE', idType: null, id: null, name: null, count: 1 },
      ],
    },
  ],
  deferredIndicators: testConfiguration.deferredIndicators,
};
