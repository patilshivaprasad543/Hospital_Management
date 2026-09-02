// SmartCare 360 Service Worker
const CACHE_NAME = 'smartcare360-v1';
const PRECACHE_ASSETS = [
  '/',
  '/css/style.css?v=5.0',
  '/images/icons/icon-192.png',
  '/images/icons/icon-512.png',
  '/images/icons/icon.svg'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(PRECACHE_ASSETS).catch((err) => {
        console.warn('Pre-caching non-fatal warning:', err);
      });
    }).then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) {
            return caches.delete(key);
          }
        })
      );
    }).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const request = event.request;

  // Only handle GET requests
  if (request.method !== 'GET') {
    return;
  }

  // Network-first for HTML pages (portal, dashboards, dynamic data)
  if (request.headers.get('accept') && request.headers.get('accept').includes('text/html')) {
    event.respondWith(
      fetch(request)
        .catch(() => caches.match(request).then((cached) => cached || caches.match('/')))
    );
    return;
  }

  // Cache-first for static assets (images, CSS, fonts)
  event.respondWith(
    caches.match(request).then((cached) => {
      return cached || fetch(request).then((response) => {
        if (response.status === 200 && (request.url.includes('/css/') || request.url.includes('/images/'))) {
          const clone = response.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(request, clone));
        }
        return response;
      });
    })
  );
});
