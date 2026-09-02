// PWA Service Worker Registration & App Install Handler
(function() {
    // 1. Register Service Worker
    if ('serviceWorker' in navigator) {
        window.addEventListener('load', function() {
            navigator.serviceWorker.register('/service-worker.js')
                .then(function(reg) {
                    console.log('SmartCare 360 PWA ServiceWorker registered with scope:', reg.scope);
                })
                .catch(function(err) {
                    console.warn('SmartCare 360 PWA ServiceWorker registration failed:', err);
                });
        });
    }

    // 2. Handle beforeinstallprompt for one-tap App install
    var deferredPrompt = null;
    window.addEventListener('beforeinstallprompt', function(e) {
        e.preventDefault();
        deferredPrompt = e;

        // Reveal install buttons if any exist on the page
        var installButtons = document.querySelectorAll('.pwa-install-trigger');
        installButtons.forEach(function(btn) {
            btn.style.display = 'inline-flex';
            btn.onclick = function() {
                if (deferredPrompt) {
                    deferredPrompt.prompt();
                    deferredPrompt.userChoice.then(function(choiceResult) {
                        if (choiceResult.outcome === 'accepted') {
                            console.log('User accepted the PWA install prompt');
                        }
                        deferredPrompt = null;
                    });
                }
            };
        });
    });

    window.addEventListener('appinstalled', function() {
        console.log('SmartCare 360 installed as an app');
        var installButtons = document.querySelectorAll('.pwa-install-trigger');
        installButtons.forEach(function(btn) {
            btn.style.display = 'none';
        });
    });
})();
