const pad = (value: number) => String(value).padStart(2, '0');

/** Data/hora local no formato aceito por <input type="datetime-local"> e pela API. */
export function toLocalIsoString(date: Date): string {
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
    `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  );
}

/** Garante segundos no timestamp (o input omite ":00" quando os segundos são zero). */
export function normalizeTimestamp(value: string): string {
  return /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value) ? `${value}:00` : value;
}

/** Sugere um eventId único o suficiente para testes manuais (ex.: EVT-482913). */
export function generateEventId(random: () => number = Math.random): string {
  return `EVT-${Math.floor(100000 + random() * 900000)}`;
}
