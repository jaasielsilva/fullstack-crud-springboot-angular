// Service worker mínimo — habilita "Adicionar à tela inicial" (PWA) sem cache offline.
self.addEventListener('install', (event) => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim());
});
