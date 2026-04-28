import { describe, expect, it } from 'vitest'

function parseStoredBankId(value) {
  return Number(value) || null
}

describe('session storage contract', () => {
  it('parses currentBankId for question bank isolation', () => {
    expect(parseStoredBankId('7')).toBe(7)
    expect(parseStoredBankId('')).toBeNull()
  })
})
