import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { apiClient } from '../api/client'
import { server } from './mocks/server'

// jsdom's XMLHttpRequest (axios's default adapter whenever `window` exists)
// never resolves/rejects when the request body is a FormData containing a
// real File/Blob - a jsdom limitation, not something real browsers hit. Only
// in tests, switch the shared axios instance to the fetch adapter (Node's
// native fetch handles Blob bodies correctly) with an absolute base URL,
// since axios's fetch adapter - unlike its XHR adapter - can't resolve a
// relative baseURL against the current page location.
apiClient.defaults.adapter = 'fetch'
apiClient.defaults.baseURL = `${window.location.origin}/api`

// Recent Node versions ship a global `localStorage` backed by a SQLite file
// that must be configured via `--localstorage-file`; without it, the global
// exists but its methods are non-functional stubs. Because that global is
// already defined before jsdom's environment sets up `window`, jsdom skips
// installing its own working implementation. Replace it with a simple
// in-memory Storage so localStorage-dependent code (auth token/session
// persistence) works the same way it does in a real browser.
function createMemoryStorage(): Storage {
  const store = new Map<string, string>()
  return {
    get length() {
      return store.size
    },
    clear: () => store.clear(),
    getItem: (key: string) => (store.has(key) ? (store.get(key) as string) : null),
    key: (index: number) => Array.from(store.keys())[index] ?? null,
    removeItem: (key: string) => {
      store.delete(key)
    },
    setItem: (key: string, value: string) => {
      store.set(key, String(value))
    },
  } as Storage
}

const memoryStorage = createMemoryStorage()
for (const target of [globalThis, window] as const) {
  Object.defineProperty(target, 'localStorage', {
    value: memoryStorage,
    configurable: true,
    writable: true,
  })
}

// ResizeObserver isn't implemented in jsdom; Recharts' ResponsiveContainer
// needs it to measure its container.
class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
if (typeof globalThis.ResizeObserver === 'undefined') {
  globalThis.ResizeObserver = ResizeObserverStub as unknown as typeof ResizeObserver
}

if (!window.matchMedia) {
  window.matchMedia = ((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  })) as typeof window.matchMedia
}

beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }))
afterEach(() => {
  cleanup()
  server.resetHandlers()
  window.localStorage.clear()
})
afterAll(() => server.close())
