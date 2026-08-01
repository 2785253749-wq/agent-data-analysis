import type { AnalysisResponse } from '@/api/datasets'

export interface ReportDoc {
  id: string
  title: string
  notes: string
  question: string
  createdAt: string
  result: AnalysisResponse
}

const LATEST_KEY = 'analysis-latest-v1'
const REPORTS_KEY = 'analysis-reports-v1'

function readJSON<T>(key: string): T | null {
  try {
    const raw = localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : null
  } catch {
    return null
  }
}

function writeJSON(key: string, value: unknown) {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // quota exceeded — ignore
  }
}

/** Truncate queryResult rows to ≤200 before persisting (localStorage quota safety). */
function truncateResult(r: AnalysisResponse): AnalysisResponse {
  if (r.queryResult && r.queryResult.rows.length > 200) {
    return {
      ...r,
      queryResult: { ...r.queryResult, rows: r.queryResult.rows.slice(0, 200) },
    }
  }
  return r
}

export function saveLatestAnalysis(r: AnalysisResponse): void {
  writeJSON(LATEST_KEY, truncateResult(r))
}

export function loadLatestAnalysis(): AnalysisResponse | null {
  return readJSON<AnalysisResponse>(LATEST_KEY)
}

export function getReports(): ReportDoc[] {
  return readJSON<ReportDoc[]>(REPORTS_KEY) ?? []
}

export function saveReport(doc: ReportDoc): ReportDoc[] {
  const all = [doc, ...getReports()]
  writeJSON(REPORTS_KEY, all)
  return all
}

export function deleteReport(id: string): ReportDoc[] {
  const all = getReports().filter((d) => d.id !== id)
  writeJSON(REPORTS_KEY, all)
  return all
}

export function makeId(): string {
  return crypto.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(36).slice(2)}`
}
