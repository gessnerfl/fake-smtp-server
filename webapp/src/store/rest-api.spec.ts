import { setupEmailEventSource } from './rest-api';

describe('setupEmailEventSource', () => {
  const createStore = () => ({
    getState: () => ({ auth: { isAuthenticated: true } }),
    dispatch: jest.fn(),
  });

  const flushMicrotasks = async () => {
    await Promise.resolve();
    await Promise.resolve();
  };

  const createSseResponse = (chunks: string[]): Response => {
    const encoder = new TextEncoder();
    const stream = new ReadableStream<Uint8Array>({
      start(controller) {
        chunks.forEach((chunk) => controller.enqueue(encoder.encode(chunk)));
        controller.close();
      },
    });
    return new Response(stream, {
      status: 200,
      headers: { 'Content-Type': 'text/event-stream' },
    });
  };

  afterEach(() => {
    if (window.emailEventSource) {
      window.emailEventSource.close();
      window.emailEventSource = undefined;
    }
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  it('schedules reconnect for authenticated SSE errors before readyState closes', () => {
    jest.useFakeTimers();
    const pendingFetch = new Promise<Response>(() => undefined);
    jest.spyOn(global, 'fetch').mockImplementation(() => pendingFetch);
    jest.spyOn(console, 'error').mockImplementation(() => undefined);
    const setTimeoutSpy = jest.spyOn(window, 'setTimeout');

    const eventSource = setupEmailEventSource(createStore(), true);
    expect(eventSource).toBeDefined();
    expect(eventSource?.onerror).toBeDefined();

    eventSource!.readyState = EventSource.OPEN;
    eventSource!.onerror!(new Event('error'));

    expect(setTimeoutSpy).toHaveBeenCalledTimes(1);
  });

  it('delays heartbeat reconnects until the configured healthy timeout is exceeded', async () => {
    jest.useFakeTimers();
    const pendingFetch = new Promise<Response>(() => undefined);
    const fetchSpy = jest.spyOn(global, 'fetch').mockImplementation(() => pendingFetch);
    jest.spyOn(console, 'error').mockImplementation(() => undefined);

    setupEmailEventSource(createStore(), {
      authenticationEnabled: true,
      sseHeartbeatIntervalSeconds: 40,
    });

    expect(fetchSpy).toHaveBeenCalledTimes(1);

    jest.advanceTimersByTime(70_000);
    await flushMicrotasks();
    expect(fetchSpy).toHaveBeenCalledTimes(1);

    jest.advanceTimersByTime(20_000);
    await flushMicrotasks();
    expect(fetchSpy).toHaveBeenCalledTimes(1);

    jest.advanceTimersByTime(1_500);
    await flushMicrotasks();

    expect(fetchSpy).toHaveBeenCalledTimes(2);
  });

  it('reconnects authenticated SSE stream after retry delay', () => {
    jest.useFakeTimers();
    const pendingFetch = new Promise<Response>(() => undefined);
    const fetchSpy = jest.spyOn(global, 'fetch').mockImplementation(() => pendingFetch);
    jest.spyOn(console, 'error').mockImplementation(() => undefined);
    jest.spyOn(window, 'setInterval').mockImplementation(() => 1 as unknown as ReturnType<typeof window.setInterval>);

    const firstEventSource = setupEmailEventSource(createStore(), true);
    expect(firstEventSource).toBeDefined();
    expect(fetchSpy).toHaveBeenCalledTimes(1);

    firstEventSource!.readyState = EventSource.OPEN;
    firstEventSource!.onerror!(new Event('error'));

    jest.runOnlyPendingTimers();

    const secondEventSource = window.emailEventSource;
    expect(secondEventSource).toBeDefined();
    expect(secondEventSource).not.toBe(firstEventSource);
    expect(fetchSpy).toHaveBeenCalledTimes(2);
  });

  it('reconnects after stream closes and processes events from the new stream', async () => {
    jest.useFakeTimers();
    jest.spyOn(console, 'error').mockImplementation(() => undefined);
    jest.spyOn(console, 'log').mockImplementation(() => undefined);
    jest.spyOn(window, 'setInterval').mockImplementation(() => 1 as unknown as ReturnType<typeof window.setInterval>);

    const store = createStore();
    let fetchCallCount = 0;
    const fetchSpy = jest.spyOn(global, 'fetch').mockImplementation(() => {
      fetchCallCount += 1;
      if (fetchCallCount === 1) {
        return Promise.resolve(createSseResponse([]));
      }
      if (fetchCallCount === 2) {
        return Promise.resolve(createSseResponse([
          'event: connection-established\ndata: ok\n\n',
          'event: email-received\ndata: 42\n\n',
        ]));
      }
      return new Promise<Response>(() => undefined);
    });

    setupEmailEventSource(store, true);
    for (let i = 0; i < 5 && fetchSpy.mock.calls.length < 2; i += 1) {
      await flushMicrotasks();
      jest.advanceTimersByTime(10_000);
    }

    for (let i = 0; i < 5 && store.dispatch.mock.calls.length === 0; i += 1) {
      await flushMicrotasks();
    }

    expect(fetchSpy).toHaveBeenCalledTimes(2);
    expect(store.dispatch).toHaveBeenCalled();
  });

  it('processes CRLF-delimited SSE events for authenticated streams', async () => {
    jest.useFakeTimers();
    jest.spyOn(console, 'error').mockImplementation(() => undefined);
    jest.spyOn(console, 'log').mockImplementation(() => undefined);
    jest.spyOn(window, 'setInterval').mockImplementation(() => 1 as unknown as ReturnType<typeof window.setInterval>);

    const store = createStore();
    const pendingFetch = new Promise<Response>(() => undefined);
    let fetchCallCount = 0;
    jest.spyOn(global, 'fetch').mockImplementation(() => {
      fetchCallCount += 1;
      if (fetchCallCount === 1) {
        return Promise.resolve(createSseResponse([
          'event: connection-established\r\ndata: ok\r\n\r\n',
          'event: email-received\r\ndata: 42\r\n\r\n',
        ]));
      }
      return pendingFetch;
    });

    setupEmailEventSource(store, true);
    for (let i = 0; i < 5 && store.dispatch.mock.calls.length === 0; i += 1) {
      await flushMicrotasks();
      jest.advanceTimersByTime(10_000);
    }

    expect(store.dispatch).toHaveBeenCalled();
  });
});
