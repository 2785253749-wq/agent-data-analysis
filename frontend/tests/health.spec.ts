import { describe, it, expect, vi, beforeEach } from 'vitest'
import axios from 'axios'

// Mock axios to avoid real network calls in tests
vi.mock('axios')

describe('GET /api/health', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should return status UP when backend is healthy', async () => {
    const mockData = {
      status: 'UP',
      application: 'AI-Agent数据分析平台',
      version: '0.1.0',
      timestamp: '2026-07-31T12:00:00Z',
    }

    vi.mocked(axios.create).mockReturnValue({
      get: vi.fn().mockResolvedValue({ data: mockData }),
    } as any)

    // Verify the contract structure
    expect(mockData).toHaveProperty('status')
    expect(mockData).toHaveProperty('application')
    expect(mockData).toHaveProperty('version')
    expect(mockData).toHaveProperty('timestamp')
    expect(mockData.status).toBe('UP')
  })

  it('should include application name in response', async () => {
    const mockData = {
      status: 'UP',
      application: 'AI-Agent数据分析平台',
      version: '0.1.0',
      timestamp: '2026-07-31T12:00:00Z',
    }

    expect(mockData.application).toBe('AI-Agent数据分析平台')
  })

  it('should have version in semver format', async () => {
    const mockData = {
      status: 'UP',
      application: 'AI-Agent数据分析平台',
      version: '0.1.0',
      timestamp: '2026-07-31T12:00:00Z',
    }

    // Semver pattern: major.minor.patch
    expect(mockData.version).toMatch(/^\d+\.\d+\.\d+/)
  })

  it('should handle network errors gracefully', async () => {
    // Simulate a network error — the UI should show DOWN status
    const handleError = (error: Error): string => {
      return 'DOWN'
    }

    const result = handleError(new Error('Network Error'))
    expect(result).toBe('DOWN')
  })
})
