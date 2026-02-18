async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const opts: RequestInit = {
    method,
    headers: { 'Content-Type': 'application/json' },
  };
  if (body !== undefined) {
    opts.body = JSON.stringify(body);
  }
  const res = await fetch(`/api${path}`, opts);
  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText);
    throw new Error(`${method} ${path} failed (${res.status}): ${text}`);
  }
  const contentType = res.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    return res.json() as Promise<T>;
  }
  return undefined as unknown as T;
}

async function uploadRequest<T>(path: string, file: File, fields?: Record<string, string>): Promise<T> {
  const formData = new FormData();
  formData.append('file', file);
  if (fields) {
    for (const [key, value] of Object.entries(fields)) {
      formData.append(key, value);
    }
  }
  const res = await fetch(`/api${path}`, {
    method: 'POST',
    body: formData,
  });
  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText);
    throw new Error(`POST ${path} failed (${res.status}): ${text}`);
  }
  const contentType = res.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    return res.json() as Promise<T>;
  }
  return undefined as unknown as T;
}

const api = {
  get<T = any>(path: string): Promise<T> {
    return request<T>('GET', path);
  },
  post<T = any>(path: string, body?: unknown): Promise<T> {
    return request<T>('POST', path, body);
  },
  put<T = any>(path: string, body?: unknown): Promise<T> {
    return request<T>('PUT', path, body);
  },
  del<T = any>(path: string): Promise<T> {
    return request<T>('DELETE', path);
  },
  upload<T = any>(path: string, file: File, fields?: Record<string, string>): Promise<T> {
    return uploadRequest<T>(path, file, fields);
  },
};

export default api;
